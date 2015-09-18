import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.dictionary.*;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class TranscriptAlignment {
    public final List<WordAlignment> words;
    public final Map<Long, FrameAlignment> frames;
    public final List<TimeFrame> confusionTimeFrames;
    public final long lastFrame;
    public boolean badTranscript = false;

    public TranscriptAlignment(String transcript,
            List<WordResult> wordResults,
            List<SpeechClassifiedData> speechData,
            List<FloatData> features) {
        this.words = getWordAlignments(transcript, wordResults);
        this.frames = getFrameAlignments(this.words, speechData, features);
        this.confusionTimeFrames = getConfusionTimeFrames(this.words);
        this.lastFrame = Collections.max(frames.keySet());

        // Flag this transcript if no words could be aligned.
        this.badTranscript = (wordResults.size() == 0);
    }

    /**
     * Gets a list of segments representing a segmentation of this alignment.
     * Segments are guaranteed to be split between identified words so that
     * unidentified words do not end up on segment boundaries.
     * Segments may not be contiguous, but any missing regions are sure to
     * be void of any useful information.
     *
     * @return list of segments
     */
    public List<Segment> getSegments() {
        List<Segment> segments = new ArrayList<Segment>();
        List<TimeFrame> empty = getEmptyRegions(150);

        // If this is a bad transcript, make no attempts at segmentation.
        if (this.badTranscript == true) {
            return segments;
        }

        // If there are no empty regions, return a single segment.
        if (empty.size() == 0) {
            Segment s = new Segment(null, new TimeFrame(0, this.lastFrame), null);
            s.words = this.words;
            segments.add(s);
            return segments;
        }

        TimeFrame first = empty.get(0);
        segments.add(new Segment(
                    null,
                    new TimeFrame(0, first.getStart()),
                    first));

        // Create segments from empty regions.
        for (int i = 1; i < empty.size(); ++i) {
            TimeFrame left = empty.get(i-1);
            TimeFrame right = empty.get(i);
            long start = left.getEnd() + 10;
            long end = right.getStart() - 10;

            TimeFrame time = new TimeFrame(start, end);
            segments.add(new Segment(left, time, right));
        }

        // Add last one.
        TimeFrame last = empty.get(empty.size() - 1);
        segments.add(new Segment(
                    last,
                    new TimeFrame(last.getEnd(), this.lastFrame),
                    null));

        // Assign words to segments.
        int index = 0;
        Segment curr = segments.get(index);

        for (WordAlignment word : this.words) {
            if (word.time == null || word.time.getStart() <= curr.getEnd()) {
                curr.words.add(word);
            } else if (index == segments.size() - 1) {
                // Shouldn't happen, but...
                System.err.println("WARNING: word not added to segment");
                break;
            } else {
                curr = segments.get(++index);
                curr.words.add(word);
            }
        }

        List<Segment> output = new ArrayList<Segment>();

        // Filter empty segments.
        for (Segment s : segments)
            if (s.words.size() > 0)
                output.add(s);

        // Merge segments, threshold = 5000ms.
        output = Segment.merge(output, 5000);
        return output;
    }

    /**
     * Validates a time frame by checking to see if it lies between two
     * identified words or not.
     * Non-speech regions are only valid if they split over words that have
     * identified time frames.  Otherwise, segments may start or end with
     * unidentified words, and we can't be certain the divide didn't split
     * a word in half.
     *
     * @param timeFrame non speech time frame
     * @return whether the time frame is valid.
     */
    private boolean validateTimeFrame(TimeFrame timeFrame) {
        if (timeFrame.getStart() == 0 || timeFrame.getEnd() == this.lastFrame)
            return false;

        // Check for overlapping confusion areas.
        for (TimeFrame confusion : this.confusionTimeFrames) {
            if (confusion.getStart() > timeFrame.getEnd() ||
                    confusion.getEnd() < timeFrame.getStart())
                continue;
            else return false;
        }

        // Check word regions.
        for (WordAlignment word : this.words) {
            TimeFrame time = word.time;
            if (time != null) {
                if (time.getStart() > timeFrame.getEnd() ||
                        time.getEnd() < timeFrame.getStart())
                    continue;
                else return false;
            }
        }
 
        return true;
    } 

    /**
     * Finds all regions of frames that do not have corresponding words and are
     * tagged as non-speech.
     *
     * @param threshold threshold for length of contiguous region
     * @return list of TimeFrames
     */
    public List<TimeFrame> getEmptyRegions(long threshold) {
        List<FrameAlignment> emptyFrames = new ArrayList<FrameAlignment>();
        List<TimeFrame> nonSpeech = new ArrayList<TimeFrame>();

        for (FrameAlignment frame : frames.values()) {
            if (frame.isEmpty()) emptyFrames.add(frame);
        }

        long start = emptyFrames.get(0).time;
        long end = start;
        for (FrameAlignment frame : emptyFrames) {
            if (frame.time == end + 10) {
                end = frame.time;
            } else if (frame.time > end + 10) {
                if (end != start && end - start >= threshold) {
                    TimeFrame timeFrame = new TimeFrame(start, end);
                    if (validateTimeFrame(timeFrame))
                        nonSpeech.add(timeFrame);
                    //System.out.printf("%f,%f,%d\n", start / 1000.0, end / 1000.0, end-start);
                }
                start = end = frame.time;
            }
        }

        return nonSpeech;
    }

    /**
     * Creates a list of Word Alignments from a list of Word Result objects
     * and the transcript they were aligned to.
     * Creates WordAlignment objects without corresponding words for words
     * that were not found during the alignment process.
     *
     * @param transcript transcript
     * @param wordResults word results from alignment process
     * @return list of word alignments
     */
    private List<WordAlignment> getWordAlignments(String transcript,
            List<WordResult> wordResults) {
        List<WordAlignment> wordAlignments = new ArrayList<WordAlignment>();
        SpeechAligner aligner = SpeechTools.getSpeechAligner();

        // Align transcript
        List<String> stringResults = new ArrayList<String>();
        List<String> sentences = aligner.getTokenizer().expand(transcript);
        List<String> words = aligner.sentenceToWords(sentences);

        if (wordResults.size() == 0) {
            for (String w : words) {
                wordAlignments.add(new WordAlignment(w, null));
            }
            return wordAlignments;
        }

        for (WordResult wr : wordResults) {
            stringResults.add(wr.getWord().getSpelling());
        }
        
        LongTextAligner textAligner =
                new LongTextAligner(stringResults, 2);
        int[] aid = textAligner.align(words);

        int lastId = -1;
        for (int i = 0; i < aid.length; ++i) {
            if (aid[i] == -1) {
                Word word = null;
                wordAlignments.add(new WordAlignment(words.get(i), null));
            } else {
                WordResult wr = wordResults.get(aid[i]);
                wordAlignments.add(new WordAlignment(wr.getWord().toString(), wr));
                lastId = aid[i];
            }
        }

        return wordAlignments;
    }

    /**
     * Creates a map of collect times to FrameAlignment objects from a list of
     * WordAlignment objects.
     * Blank alignments are create for frames that do not have corresponding
     * words, and frames are tagged as speech/non-speech according to the given
     * speech data. Finally, each alignment is given its corresponding feature
     * data.
     * 
     * @param words word alignments
     * @param speechData speech classified data
     * @param features extracted features by frame
     * @return map of times to frame alignments
     */
    private Map<Long, FrameAlignment> getFrameAlignments(List<WordAlignment> words,
            List<SpeechClassifiedData> speechData,
            List<FloatData> features) {
        // Populate frame alignments from words.
        Map<Long, FrameAlignment> tempFrames = new LinkedHashMap<Long, FrameAlignment>();

        for (WordAlignment word : words) {
            for (FrameAlignment frame : word.frames) {
                tempFrames.put(frame.time, frame);
                frame.isSpeech = true;
            }
        }

        // Fill in the gaps and set speech status.
        Map<Long, FrameAlignment> frames = new LinkedHashMap<Long, FrameAlignment>();
        long time = 0;

        for (SpeechClassifiedData data : speechData) {
            time = data.getCollectTime();
            FrameAlignment f = tempFrames.get(time);
            if (f == null)
                f = new FrameAlignment(time);
            f.isSpeech = data.isSpeech();
            frames.put(time, f);
        }

        tempFrames = frames;
        frames = new LinkedHashMap<Long, FrameAlignment>();

        for (FloatData data : features) {
            time = data.getCollectTime();
            FrameAlignment f = tempFrames.get(time);
            if (f == null)
                f = new FrameAlignment(time);
            f.features = data;
            frames.put(time, f);
        }

        return frames;
    }

    public List<TimeFrame> getConfusionTimeFrames(List<WordAlignment> wordAlignments) {
        List<TimeFrame> confusion = new ArrayList<TimeFrame>();
        if (wordAlignments.size() == 0) return confusion;

        long lastEnd = 0;
        List<WordAlignment> confusionWords = new ArrayList<WordAlignment>();
        WordAlignment word = wordAlignments.get(0);
        boolean inConfusion = (word.time == null);

        for (int i = 1; i < wordAlignments.size(); ++i) {
            word = wordAlignments.get(i);
            if (word.time == null) {
                inConfusion = true;
            } else {
                if (inConfusion) {
                    confusion.add(new TimeFrame(lastEnd, word.time.getStart()));
                }
                lastEnd = word.time.getEnd();
                inConfusion = false;
            }
        }
        if (inConfusion) {
            confusion.add(new TimeFrame(lastEnd, this.lastFrame));
        }
        return confusion;
    }
}
