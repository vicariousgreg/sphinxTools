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

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class SpeechTools {
    private static final String ACOUSTIC_MODEL_PATH =
            "/audio/models/transcription/acoustic/models/en-us";
    private static final String DICTIONARY_PATH =
            "/audio/models/transcription/dictionary/cmudict-en-us.dict";
            //"/audio/models/transcription/en_us_nostress/cmudict-5prealpha.dict";
    private static final String G2P_PATH =
            //null;
            "/audio/models/transcription/en_us_nostress/model.fst.ser";
    private static Context context;

    ///////////////////
    /* Sphinx tools. */
    ///////////////////

    /**
     * Gets the Context.
     * @return context
     */
    public static Context getContext() {
        if (context == null) {
            Configuration config = new Configuration();
            config.setAcousticModelPath(ACOUSTIC_MODEL_PATH);
            config.setDictionaryPath(DICTIONARY_PATH);
            try {
                context = new Context(config);
            } catch (Exception e) {
                System.err.println(e);
                System.exit(-1);
                return null;
            }
        }
        return context;
    }



    /////////////////////////////////
    /* Low level extraction tools. */
    /////////////////////////////////

    /**
     * Extracts data signals from an input stream.
     * @param audioUrl url of audio file
     * @return list of signals
     */
    public static List<Signal> getSignals(URL audioUrl) throws Exception {
        List<Signal> out = new ArrayList<Signal>();

        Context context = getContext();
        context.setLocalProperty("trivialScorer->frontend", "liveFrontEnd");
        context.setSpeechSource(audioUrl.openStream());
        FrontEnd frontEnd = context.getInstance(FrontEnd.class);

        Data data = null;
        while ((data = frontEnd.getData()) != null) {
            if (data instanceof Signal) {
                out.add((Signal) data);
            }
        }
        return out;
    }

    /**
     * Extracts feature data from an input stream.
     * @param audioUrl url of audio file
     * @param classifySpeech whether to include speech classification
     * @return list of feature data
     */
    public static List<FloatData> getFeatures(URL audioUrl, boolean classifySpeech) throws Exception {
        List<FloatData> out = new ArrayList<FloatData>();

        Context context = getContext();
        if (classifySpeech) {
            context.setLocalProperty("trivialScorer->frontend", "unmarkedFrontEnd");
        } else {
            context.setLocalProperty("trivialScorer->frontend", "liveFrontEnd");
        }
        context.setSpeechSource(audioUrl.openStream());
        FrontEnd frontEnd = context.getInstance(FrontEnd.class);

        Data data = null;
        while ((data = frontEnd.getData()) != null) {
            if (data instanceof FloatData) {
                out.add((FloatData) data);
            }
        }
        return out;
    }


    //////////////////////////////
    /* Higher level processing. */
    //////////////////////////////

    /**
     * Creates a SenoneDump containing acoustic scores for each Senone for
     * each frame in the input stream.
     * @param audioUrl url of audio file
     * @return senone dump
     */
    public static SenoneDump getSenoneDump(URL audioUrl) throws Exception {
        SenoneDump dmp = new SenoneDump(getContext().getLoader());
        for (Data data : getFeatures(audioUrl, false)) {
            if (data instanceof FloatData) {
                dmp.addFrame(data);
            }
        }
        return dmp;
    }

    //////////////////////
    /* Alignment tools. */
    //////////////////////

    /**
     * Creates a high level alignment, including word and signal alignment.
     * @param audioUrl audio location
     * @param transcriptPath transcript path
     * @return alignment
     */
    public static Alignment getAlignment(URL audioUrl, String transcriptPath) throws Exception {
        // Load transcript
        Scanner scanner = new Scanner(new File(transcriptPath));  
        scanner.useDelimiter("\\Z");  
        String transcript = scanner.next();
        scanner.close();

        return new Alignment(getWordAlignment(audioUrl, transcript),
                getSignals(audioUrl), getFeatures(audioUrl, false));
    }

    /**
     * Creates a word alignment given audio and a transcript.
     * @param audioUrl audio location
     * @param transcript transcript
     * @return word alignment list
     */
    public static List<WordResult> getWordAlignment(URL audioUrl, String transcript) throws Exception {
        SpeechAligner aligner =
                new SpeechAligner(ACOUSTIC_MODEL_PATH, DICTIONARY_PATH, G2P_PATH);
        aligner.setTokenizer(new USEnglishTokenizer());

        return aligner.align(audioUrl, transcript);
    }

    /**
     * Creates a text alignment given a word alignment and transcript.
     * @param wordResults word alignment
     * @param transcript transcript
     * @return text alignment
     */
    public static List<String> getTextAlignment(List<WordResult> wordResults, String transcript) throws Exception {
        List<String> output = new ArrayList<String>();

        List<String> stringResults = new ArrayList<String>();
        for (WordResult wr : wordResults) {
            stringResults.add(wr.getWord().getSpelling());
        }

        SpeechAligner aligner =
                new SpeechAligner(ACOUSTIC_MODEL_PATH, DICTIONARY_PATH, G2P_PATH);
        aligner.setTokenizer(new USEnglishTokenizer());

        LongTextAligner textAligner =
                new LongTextAligner(stringResults, 2);
        List<String> sentences = aligner.getTokenizer().expand(transcript);
        List<String> words = aligner.sentenceToWords(sentences);
        
        int[] aid = textAligner.align(words);
        
        int lastId = -1;
        for (int i = 0; i < aid.length; ++i) {
            if (aid[i] == -1) {
                //System.out.format("- %s\n", words.get(i));
                output.add(String.format("- %s\n", words.get(i)));
            } else {
                if (aid[i] - lastId > 1) {
                    for (WordResult result : wordResults.subList(lastId + 1,
                            aid[i])) {
                        //System.out.format("+ %-25s [%s]\n", result.getWord()
                         //       .getSpelling(), result.getTimeFrame());
                        output.add(String.format("+ %-25s [%s]\n", result.getWord()
                                .getSpelling(), result.getTimeFrame()));
                    }
                }
                //System.out.format("  %-25s [%s]\n", wordResults.get(aid[i])
                 //       .getWord().getSpelling(), wordResults.get(aid[i])
                  //      .getTimeFrame());
                output.add(String.format("  %-25s [%s]\n", wordResults.get(aid[i])
                        .getWord().getSpelling(), wordResults.get(aid[i])
                        .getTimeFrame()));
                lastId = aid[i];
            }
        }

        if (lastId >= 0 && wordResults.size() - lastId > 1) {
            for (WordResult result : wordResults.subList(lastId + 1,
                    wordResults.size())) {
                //System.out.format("+ %-25s [%s]\n", result.getWord()
                 //       .getSpelling(), result.getTimeFrame());
                output.add(String.format("+ %-25s [%s]\n", result.getWord()
                        .getSpelling(), result.getTimeFrame()));
            }
        }

        return output;
    }
}
