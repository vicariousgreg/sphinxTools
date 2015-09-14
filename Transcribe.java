/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

import java.io.InputStream;
import java.io.FileInputStream;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.instrumentation.ConfigMonitor;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.Unit;

/**
 * A simple example that shows how to transcribe a continuous audio file that
 * has multiple utterances in it.
 */
public class Transcribe {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("USAGE: java Transcribe <acoustic model> <language model> <dictionary model> <audio path>");
            System.exit(0);
        }

        Configuration configuration = new Configuration();

        configuration
                .setAcousticModelPath(args[0]);
        configuration
                .setLanguageModelPath(args[1]);
        configuration
                .setDictionaryPath(args[2]);

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);

        //System.out.println("TRANSCRIBING " + args[1]);

        // Simple recognition with generic model
        InputStream stream = new FileInputStream(args[3]);
        stream.skip(44);

        SpeechResult result;
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            System.out.println(result.getHypothesis());
            //System.out.println("Best 3 hypothesis:");
            //for (String s : result.getNbest(3))
            //    System.out.println(s);
            //Lattice lattice = result.getLattice();

            /*
            System.out.format("Hypothesis: %s\n", result.getHypothesis());

            System.out.println("List of recognized words and their times:");
            for (WordResult r : result.getWords()) {
                System.out.println(r);
            }

            System.out.println("Best 3 hypothesis:");
            for (String s : result.getNbest(3))
                System.out.println(s);

            System.out.println("Lattice contains "
                    + result.getLattice().getNodes().size() + " nodes");
            */
        }
        recognizer.stopRecognition();


        /*
        // Live adaptation to speaker with speaker profiles

        stream = new FileInputStream(args[0]);
        stream.skip(44);

        // Stats class is used to collect speaker-specific data
        Stats stats = recognizer.createStats(1);
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            stats.collect(result);
        }
        recognizer.stopRecognition();

        // Transform represents the speech profile
        Transform transform = stats.createTransform();
        recognizer.setTransform(transform);

        // Decode again with updated transform
        stream = new FileInputStream(args[0]);
        stream.skip(44);
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            System.out.format("Hypothesis: %s\n", result.getHypothesis());
        }
        recognizer.stopRecognition();
        */
    }
}
