/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

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
    public final long lastFrame;

    public TranscriptAlignment(String transcript,
            List<WordResult> wordResults,
            List<SpeechClassifiedData> speechData,
            List<FloatData> features) {
        this.words = getWordAlignments(transcript, wordResults);
        this.frames = getFrameAlignments(this.words, speechData, features);
        this.lastFrame = Collections.max(frames.keySet());
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
                    nonSpeech.add(new TimeFrame(start, end));
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

        // Align transcript
        List<String> stringResults = new ArrayList<String>();
        for (WordResult wr : wordResults) {
            stringResults.add(wr.getWord().getSpelling());
        }

        SpeechAligner aligner = SpeechTools.getSpeechAligner();

        LongTextAligner textAligner =
                new LongTextAligner(stringResults, 2);
        List<String> sentences = aligner.getTokenizer().expand(transcript);
        List<String> words = aligner.sentenceToWords(sentences);
        
        int[] aid = textAligner.align(words);

        Dictionary dictionary = SpeechTools.getDictionary();
        
        int lastId = -1;
        for (int i = 0; i < aid.length; ++i) {
            if (aid[i] == -1) {
                Word word = null;
                wordAlignments.add(new WordAlignment(dictionary.getWord(words.get(i)), null));
            } else {
                WordResult wr = wordResults.get(aid[i]);
                wordAlignments.add(new WordAlignment(wr.getWord(), wr));
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
}
