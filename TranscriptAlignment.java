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
import edu.cmu.sphinx.linguist.dictionary.*;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class TranscriptAlignment {
    public final List<WordAlignment> words;
    public final Map<Long, FrameAlignment> frames;

    public TranscriptAlignment(String transcript,
            List<WordResult> wordResults,
            List<SpeechClassifiedData> speechData) {
        this.words = new ArrayList<WordAlignment>();

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
                this.words.add(new WordAlignment(dictionary.getWord(words.get(i)), null));
            } else {
                WordResult wr = wordResults.get(aid[i]);
                this.words.add(new WordAlignment(wr.getWord(), wr));
                lastId = aid[i];
            }
        }

        // Populate frame alignments from words.
        Map<Long, FrameAlignment> tempFrames = new LinkedHashMap<Long, FrameAlignment>();

        for (WordAlignment word : this.words) {
            for (FrameAlignment frame : word.frames) {
                tempFrames.put(frame.time, frame);
                frame.isSpeech = true;
            }
        }

        // Fill in the gaps and set speech status.
        this.frames = new LinkedHashMap<Long, FrameAlignment>();

        for (SpeechClassifiedData data : speechData) {
            long time = data.getCollectTime();
            FrameAlignment f = tempFrames.get(time);
            if (f == null)
                f = new FrameAlignment(time);
            f.isSpeech = data.isSpeech();
            this.frames.put(time, f);
        }
    }
}
