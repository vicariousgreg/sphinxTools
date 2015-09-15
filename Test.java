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
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;

public class Test {
    private static final int senoneId = 4974;

    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("config.xml");

        URL audioUrl = null;
        String transcriptPath = null;
        if (args.length > 1) {
            audioUrl = new File(args[0]).toURI().toURL();
            transcriptPath = args[1];
        } else {
            System.err.println("Usage: java Test <wav> <transcript>");
            System.exit(-1);
        }

        //System.out.println(SpeechTools.getWordAlignment(audioUrl, transcriptPath).size());
        //System.out.println(SpeechTools.getSignals(audioUrl).size());

        Alignment al = SpeechTools.getAlignment(audioUrl, transcriptPath);
        //al.print();
        //al.dumpAlignment();
        //al.dumpWords();
        //al.dumpSignals();
        //al.dumpSpeech();
        al.dumpFrames();

        //SenoneDump dmp = SpeechTools.getSenoneDump(audioUrl);
        //dmp.print();
    }
}
