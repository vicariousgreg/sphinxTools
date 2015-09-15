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

public class Alignment {
    public final List<WordResult> wordResults;
    public final List<Signal> signals;
    public final List<FloatData> features;
    public final List<SpeechClassifiedData> speech;
    public final Map<Long, Frame> frames;

    public Alignment(List<WordResult> wr,
            List<Signal> sig,
            List<FloatData> feat,
            List<SpeechClassifiedData> speech) {
        this.wordResults = wr;
        this.signals = sig;
        this.features = feat;
        this.speech = speech;

        this.frames = new LinkedHashMap<Long, Frame>();
        for (SpeechClassifiedData data : speech) {
            Frame frame = new Frame(data.getCollectTime());
            frame.isSpeech = data.isSpeech();
            frames.put(data.getCollectTime(), frame);
        }

        for (WordResult result : wordResults) {
            String word = result.getWord().toString();

            for (Token token : result.getTokens()) {
                long time = token.getCollectTime();
                if (frames.get(time) != null) {
                    frames.get(time).word = word;
                    frames.get(time).senone = 
                        ((HMMSearchState) token.getSearchState())
                        .getHMMState().getHMM().getUnit().toString();
                    frames.get(time).state = 
                        ((HMMSearchState) token.getSearchState())
                        .getHMMState().getState();
                    frames.get(time).score = token.getAcousticScore();
                }
            }
        }
    }

    public void print() {
        System.out.println(wordResults.size());
        System.out.println(signals.size());
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
        for (Signal sig : signals) {
            System.out.printf("%s %d\n",
                    sig.toString(),
                    sig.getTime());
        }
    }

    public void dumpAlignment() {
        Iterator<WordResult> words  = wordResults.iterator();
        Iterator<Signal> sigs  = signals.iterator();
        WordResult wr = (words.hasNext()) ? words.next() : null;
        Signal sig = (sigs.hasNext()) ? sigs.next() : null;

        String currWord = "";
        String currUnit = "";
        String currState = "";
        String currSig = "";
        String currScore = "";

        for (FloatData fd : features) {
            long time = fd.getCollectTime();
            if (wr != null && time >= wr.getTimeFrame().getEnd()) {
                currWord = "";
                wr = (words.hasNext()) ? words.next() : null;
            } else if (wr != null && time >= wr.getTimeFrame().getStart()) {
                currWord = wr.getWord().toString();
            }

            currScore = "";
            currUnit = "";
            currState = "";
            if (currWord != "") {
                for (Token token : wr.getTokens()) {
                    if (token.getCollectTime() == time) {
                        currScore = Float.toString(token.getAcousticScore());

                        currUnit = ((HMMSearchState) token.getSearchState())
                            .getHMMState().getHMM().getUnit().toString();
                        currState = Integer.toString(
                                ((HMMSearchState) token.getSearchState())
                                .getHMMState().getState());
                        break;
                    }
                }
            }

            if (sig != null && time >= sig.getTime()) {
                currSig = sig.toString();
                sig = (sigs.hasNext()) ? sigs.next(): null;
            } else {
                currSig = "";
            }

            /*
            System.out.printf("%8d %20s %12s %s %12s %20s\n",
                    time,
                    currWord,
                    currUnit,
                    currState,
                    currScore,
                    currSig);
            */
            System.out.printf("%f,%s %s %s\n",
                    time / 1000.0,
                    currWord,
                    currUnit,
                    currState);
        }
    }

    public void dumpWords() {
        for (WordResult wr : wordResults)
            System.out.printf("%f,%f,%s\n",
                    wr.getTimeFrame().getStart() / 1000.0,
                    wr.getTimeFrame().getEnd() / 1000.0,
                    wr.getWord());
    }

    public void dumpSignals() {
        for (Signal sig : signals) {
            System.out.printf("%f,%s\n",
                    sig.getTime() / 1000.0,
                    sig.toString());
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

        public String toString() {
            return String.format(
                    "%5.2f %15s %12s %4d %12s %s",
                    time / 1000.0,
                    word,
                    senone,
                    (state == null) ? "" : state,
                    (score == null) ? "" : score.toString(),
                    isSpeech);
        }
    }
}
