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
import edu.cmu.sphinx.util.TimeFrame;

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

        Scanner scanner = new Scanner(new File(transcriptPath));  
        scanner.useDelimiter("\\Z");  
        String transcript = scanner.next();
        scanner.close();

        Context context = SpeechTools.getContext();
        context.setLocalProperty("trivialScorer->frontend", "unmarkedFrontEnd");

        //System.out.println(SpeechTools.getWordAlignment(audioUrl, transcriptPath).size());
        //System.out.println(SpeechTools.getSignals(audioUrl).size());

        //Alignment al = SpeechTools.getAlignment(audioUrl, transcriptPath);
        //al.print();
        //al.dumpAlignment();
        //al.dumpWords();
        //al.dumpSpeech();
        //al.validate();
        //al.dumpFrames();
        //al.printSegments();
        //System.out.println("***************");
        //al.printMergedSegments();
        //
        TranscriptAlignment t = SpeechTools.getTranscriptAlignment(audioUrl, transcriptPath);
        for (FrameAlignment f : t.frames.values()) {
            System.out.println(f);
        }
        for (TimeFrame time : t.getEmptyRegions(150)) {
            System.out.println(time);
        }

        //SenoneDump dmp = SpeechTools.getSenoneDump(audioUrl);
        //dmp.print();
    }
}
