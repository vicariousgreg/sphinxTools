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
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        URL audioUrl = null;
        URL transcriptUrl = null;
        if (args.length > 1) {
            audioUrl = new File(args[0]).toURI().toURL();
            transcriptUrl = new File(args[1]).toURI().toURL();
        } else {
            System.err.println("Usage: java Test <wav> <transcript>");
            System.exit(-1);
        }

        //System.out.println(SpeechTools.getWordAlignment(audioUrl, transcriptUrl).size());
        //System.out.println(SpeechTools.getSignals(audioUrl).size());

        //Alignment al = SpeechTools.getAlignment(audioUrl, transcriptUrl);
        //al.print();
        //al.dumpAlignment();
        //al.dumpWords();
        //al.dumpSpeech();
        //al.validate();
        //al.dumpFrames();
        //al.printSegments();
        //System.out.println("***************");
        //al.printMergedSegments();

        TranscriptAlignment t = SpeechTools.getTranscriptAlignment(audioUrl, transcriptUrl);
        for (FrameAlignment f : t.frames.values()) {
            System.out.println(f);
        }
        for (WordAlignment word : t.words) {
            System.out.println(word);
        }
        for (Segment s : t.getSegments()) System.out.println(s);

        //SenoneDump dmp = SpeechTools.getSenoneDump(audioUrl);
        //dmp.print();
    }
}
