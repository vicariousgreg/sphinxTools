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

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class Alignment {
    public final List<WordResult> wordResults;
    public final List<FloatData> features;
    public final List<SpeechClassifiedData> speech;
    public final Map<Long, Frame> frames;
    public final long lastFrame;

    public Alignment(List<WordResult> wr,
            List<FloatData> feat,
            List<SpeechClassifiedData> speech) {
        this.wordResults = wr;
        this.features = feat;
        this.speech = speech;

        this.frames = new LinkedHashMap<Long, Frame>();
        long last = 0;
        for (SpeechClassifiedData data : speech) {
            Frame frame = new Frame(data.getCollectTime());
            frame.isSpeech = data.isSpeech();
            frames.put(data.getCollectTime(), frame);
            last = data.getCollectTime();
        }
        this.lastFrame = last;

        for (WordResult result : wordResults) {
            String word = result.getWord().toString();

            for (Token token : result.getTokens()) {
                long time = token.getCollectTime();
                Frame frame = frames.get(time);
                if (frame != null) {
                    frame.word = word;
                    frame.senone = 
                        ((HMMSearchState) token.getSearchState())
                        .getHMMState().getHMM().getUnit().toString();
                    frame.state = 
                        ((HMMSearchState) token.getSearchState())
                        .getHMMState().getState();
                    frame.score = token.getAcousticScore();
                }
            }
        }
    }

    public void print() {
        System.out.println(wordResults.size());
        System.out.println(features.size());

        for (WordResult wr : wordResults) {
            System.out.printf("%s %d %d\n",
                    wr.getWord(),
                    wr.getTimeFrame().getStart(),
                    wr.getTimeFrame().getEnd());

            for (Token token : wr.getTokens()) {
                System.out.printf("  %10d %f\n",
                        token.getCollectTime(),
                        token.getAcousticScore());
            }

        }
    }

    public void dumpWords() {
        for (WordResult wr : wordResults) {
            System.out.printf("%f,%f,%s\n",
                    wr.getTimeFrame().getStart() / 1000.0,
                    wr.getTimeFrame().getEnd() / 1000.0,
                    wr.getWord());
        }
    }

    public void dumpSpeech() {
        boolean state = speech.get(0).isSpeech();
        long start = speech.get(0).getCollectTime();
        long end = 0;

        for (SpeechClassifiedData data : speech) {
            end = data.getCollectTime();
            if (data.isSpeech() != state) {
                System.out.printf("%f,%f,%s\n",
                        start / 1000.0,
                        end / 1000.0,
                        state);
                start = data.getCollectTime();
                state = data.isSpeech();
            }
        }
        System.out.printf("%f,%f,%s\n",
                start / 1000.0,
                end / 1000.0,
                state);
    }

    public void dumpFrames() {
        for (Frame frame : frames.values()) {
            System.out.println(frame);
        }
    }

    public void validate() {
        for (WordResult wr : wordResults) {
            if (wr.getTokens().size() > 0) {
                long start = wr.getTokens().get(0).getCollectTime();
                long end = wr.getTokens().get(wr.getTokens().size() - 1).getCollectTime();
                if (start != wr.getTimeFrame().getStart() ||
                        end != wr.getTimeFrame().getEnd()) {
                    System.out.printf("%f,%f,%s\n",
                            wr.getTimeFrame().getStart() / 1000.0,
                            wr.getTimeFrame().getEnd() / 1000.0,
                            wr.getWord());
                    System.out.printf("    %f,%f\n", start / 1000.0, end / 1000.0);
                    for (Token t : wr.getTokens())
                        System.out.println(frames.get(t.getCollectTime()));
                }
            } else {
                System.out.printf("Word result for %s has no tokens!\n", wr.getWord());
            }
        }
    }

    public List<TimeFrame> getEmptyRegions() {
        long threshold = 150;
        List<Frame> emptyFrames = new ArrayList<Frame>();
        List<TimeFrame> nonSpeech = new ArrayList<TimeFrame>();

        for (Frame frame : frames.values()) {
            if (frame.isEmpty()) emptyFrames.add(frame);
        }

        long start = emptyFrames.get(0).time;
        long end = start;
        for (Frame frame : emptyFrames) {
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

    public List<Region> getSegments() {
        List<Region> segments = new ArrayList<Region>();
        List<TimeFrame> nonSpeech = getEmptyRegions();
        TimeFrame left = null;
        TimeFrame right = null;

        for (TimeFrame tf : nonSpeech) {
            left = right;
            right = tf;
            long start = (left == null) ? 0 : left.getEnd() + 10;
            long end = right.getStart() - 10;
            TimeFrame time = new TimeFrame(start, end);
            segments.add(new Region(left, time, right));
        }

        if (right.getEnd() != lastFrame) {
            segments.add(new Region(right, new TimeFrame(right.getEnd() + 10, lastFrame), null));
        }

        int index = 0;
        Region curr = segments.get(index);
        for (WordResult wr : wordResults) {
            if (wr.getTimeFrame().getStart() < curr.time.getEnd()) {
                curr.words.add(wr);
            } else {
                curr = segments.get(++index);
                curr.words.add(wr);
            }
        }

        return segments;
    }

    public List<Region> getMergedSegments() {
        long threshold = 5000;
        List<Region> regions = getSegments();
        List<Region> mergedRegions = new ArrayList<Region>();

        if (regions.size() < 2) return regions;

        for (int i = 0; i < regions.size(); ++i) {
            Region curr = regions.get(i);
            //System.out.println("  " + curr);

            if (curr.words.size() == 0 ||
                    curr.getEnd() - curr.getStart() < threshold) {
                Region left = (mergedRegions.size() > 0) ? mergedRegions.get(mergedRegions.size()-1) : null;
                Region right = (i < regions.size() - 1) ? regions.get(i+1) : null;

                boolean useRight = (left == null);
                if (left != null && right != null)
                    useRight = (left.getLength() > right.getLength());

                if (useRight == true) {
                    //System.out.println("MERGE RIGHT");
                    //System.out.println(curr);
                    //System.out.println(right);

                    // use right
                    Region merged = mergeRegions(curr, right);

                    // Skip
                    ++i;
                    mergedRegions.add(merged);
                } else {
                    //System.out.println("MERGE LEFT");
                    //System.out.println(left);
                    //System.out.println(curr);

                    // use left
                    Region merged = mergeRegions(left, curr);

                    // Replace
                    mergedRegions.remove(mergedRegions.size()-1);
                    mergedRegions.add(merged);
                }
            } else {
                mergedRegions.add(curr);
            }
        }
        return mergedRegions;
    }

    public void printSegments() {
        for (Region r : getSegments()) {
            System.out.printf("%f,%f,", r.time.getStart() / 1000.0, r.time.getEnd() / 1000.0);
            StringBuilder sb = new StringBuilder("");
            for (WordResult wr : r.words) {
                sb.append(wr.getWord() + " ");
            }
            System.out.println(sb.toString());
        }
    }

    public void printMergedSegments() {
        for (Region r : getMergedSegments()) {
            System.out.printf("%f,%f,", r.time.getStart() / 1000.0, r.time.getEnd() / 1000.0);
            StringBuilder sb = new StringBuilder("");
            for (WordResult wr : r.words) {
                sb.append(wr.getWord() + " ");
            }
            System.out.println(sb.toString());
        }
    }


    private class Frame {
        public long time;
        public String word = "";
        public String senone = "";
        public Integer state = null;
        public Float score = null;
        public boolean isSpeech = false;

        public Frame(long time) {
            this.time = time;
        }

        public boolean isEmpty() {
            return this.isSpeech == false && this.word == "";
        }

        public String toString() {
            return String.format(
                    "%5.2f %15s %12s %4s %12s %s",
                    time / 1000.0,
                    word,
                    senone,
                    (state == null) ? "" : state.toString(),
                    (score == null) ? "" : score.toString(),
                    isSpeech);
        }
    }

    private static class Region {
        public TimeFrame left;
        public TimeFrame right;
        public TimeFrame time;
        public List<WordResult> words;

        public Region(TimeFrame left, TimeFrame time, TimeFrame right) {
            this.left = left;
            this.time = time;
            this.right = right;
            this.words = new ArrayList<WordResult>();
        }

        public long getStart() {
            return time.getStart();
        }

        public long getEnd() {
            return time.getEnd();
        }

        public long getContextStart() {
            return (left == null) ? time.getStart() : left.getStart();
        }

        public long getContextEnd() {
            return (right == null) ? time.getEnd() : right.getEnd();
        }

        public long getLength() {
            return getEnd() - getStart();
        }

        public String toString() {
            return String.format("%d %d", getStart(), getEnd());
        }

    }

    public static Region mergeRegions(Region leftRegion, Region rightRegion) {
        Region merged = new Region(leftRegion.left,
                new TimeFrame(leftRegion.time.getStart(), rightRegion.time.getEnd()),
                rightRegion.right);

        // Add words
        for (WordResult wr : leftRegion.words) merged.words.add(wr);
        for (WordResult wr : rightRegion.words) merged.words.add(wr);

        return merged;
    }

}
