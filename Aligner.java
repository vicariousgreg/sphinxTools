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
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;

public class Aligner {
    private static final String ACOUSTIC_MODEL_PATH =
            "/audio/models/transcription/acoustic/models/en-us";
    private static final String DICTIONARY_PATH =
            //"/audio/models/transcription/dictionary/cmudict-en-us.dict";
            "/audio/models/transcription/en_us_nostress/cmudict-5prealpha.dict";
    private static final String G2P_PATH =
            //null;
            "/audio/models/transcription/en_us_nostress/model.fst.ser";

    private static final int senoneId = 4974;

    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("config.xml");

        URL audioUrl = null;
        String transcript = null;
        if (args.length > 1) {
            audioUrl = new File(args[0]).toURI().toURL();
            Scanner scanner = new Scanner(new File(args[1]));  
            scanner.useDelimiter("\\Z");  
            transcript = scanner.next();
            scanner.close();
        } else {
            System.err.println("Usage: java Aligner <wav> <transcript> [<acoustic> <dictionary>]");
            System.exit(-1);
        }

        String acousticModelPath =
                (args.length > 2) ? args[2] : ACOUSTIC_MODEL_PATH;
        String dictionaryPath = (args.length > 3) ? args[3] : DICTIONARY_PATH;

        Configuration config = new Configuration();
        config.setAcousticModelPath(acousticModelPath);
        config.setDictionaryPath(dictionaryPath);
        Context context = new Context(config);
        Sphinx3Loader loader = ((Sphinx3Loader) context.getLoader());

        Senone s = loader.getSenonePool().get(senoneId);

        FrontEnd frontend = context.getInstance(FrontEnd.class);
        context.setSpeechSource(audioUrl.openStream());
        Data data = null;
        while ((data = frontend.getData()) != null) {
            //System.out.println(data.getClass().getName());
            if (data instanceof Signal) {
                System.out.print(((Signal) data).getTime() + ",");
                System.out.println(LogMath.LOG_ZERO);
            } else if (data instanceof FloatData) {
                System.out.print(((FloatData) data).getCollectTime() + ",");
                System.out.println(s.getScore(data));
            }
        }
        System.exit(0);


        ////////////////

        SpeechAligner aligner =
                new SpeechAligner(acousticModelPath, dictionaryPath, G2P_PATH);
        aligner.setTokenizer(new USEnglishTokenizer());

        List<WordResult> results = aligner.align(audioUrl, transcript);
        List<String> stringResults = new ArrayList<String>();

        float total = LogMath.getLogMath().linearToLog(0.0f);
        int count = 0;

        for (WordResult wr : results) {
            stringResults.add(wr.getWord().getSpelling());
            System.out.println(wr);

            for (Token token : wr.getTokens()) {
                if (Double.compare(token.getAcousticScore(), 0.0) != 0) {
                    long collectTime = token.getCollectTime();
                    float logAcousticScore = token.getAcousticScore();
                    double linearAcousticScore = LogMath.getLogMath().logToLinear(logAcousticScore);
                    char state = ' ';

                    // Add acoustic score to total.
                    total = LogMath.getLogMath().addAsLinear(total, token.getAcousticScore());
                    count += 1;

                    // Extract HMM string.
                    String searchState = token.getSearchState().toString();

                    if (searchState.charAt(0) == '*') {
                        state = searchState.charAt(2);
                    } else {
                        state = searchState.charAt(searchState.length() - 1);
                    }

                    searchState = searchState.substring(searchState.indexOf("[") - 2);
                    searchState = searchState.substring(searchState.indexOf("[") - 2);
                    if (!Character.isLetter(searchState.charAt(0))) {
                        searchState = searchState.substring(1);
                    }
                    searchState = searchState.substring(0, searchState.indexOf("]") + 1);

                    // Print data.
                    System.out.printf("%10d ", collectTime);
                    System.out.printf("%10s S%c ", searchState, state);
                    System.out.println(linearAcousticScore);

                    /*
                    Data data = token.getData();
                    for (int i = 0; i < loader.getSenonePool().size(); ++i) {
                        Senone senone = loader.getSenonePool().get(i);
                        System.out.print(senone.getScore(data));
                        System.out.println(" " + senone);
                    }
                    System.exit(0);
                    */


                    // Get Gaussian Mixture.
                    int mId = (int) ((HMMSearchState) token.getSearchState()).getHMMState().getMixtureId();
                    GaussianMixture gm = ((GaussianMixture) loader.getSenonePool().get(mId));
                    System.out.println("mId: " + mId);
                    //System.out.println(LogMath.getLogMath().logToLinear(gm.getScore(token.getData())));
                }
            }
            System.out.println();
            System.out.println();
        }

        System.out.println("Count: " + count);
        System.out.println("Log total: " + total);
        System.out.println("Log average: " + total / count);
        System.out.println("Linear total: " + LogMath.getLogMath().logToLinear(total));
        System.out.println("Linear average: " + LogMath.getLogMath().logToLinear(total) / count);
        


        LongTextAligner textAligner =
                new LongTextAligner(stringResults, 2);
        List<String> sentences = aligner.getTokenizer().expand(transcript);
        List<String> words = aligner.sentenceToWords(sentences);
        
        int[] aid = textAligner.align(words);
        
        int lastId = -1;
        for (int i = 0; i < aid.length; ++i) {
            if (aid[i] == -1) {
                System.out.format("- %s\n", words.get(i));
            } else {
                if (aid[i] - lastId > 1) {
                    for (WordResult result : results.subList(lastId + 1,
                            aid[i])) {
                        System.out.format("+ %-25s [%s]\n", result.getWord()
                                .getSpelling(), result.getTimeFrame());
                    }
                }
                System.out.format("  %-25s [%s]\n", results.get(aid[i])
                        .getWord().getSpelling(), results.get(aid[i])
                        .getTimeFrame());
                lastId = aid[i];
            }
        }

        if (lastId >= 0 && results.size() - lastId > 1) {
            for (WordResult result : results.subList(lastId + 1,
                    results.size())) {
                System.out.format("+ %-25s [%s]\n", result.getWord()
                        .getSpelling(), result.getTimeFrame());
            }
        }
    }
}
