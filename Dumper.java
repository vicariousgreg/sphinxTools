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
import edu.cmu.sphinx.util.BatchFile;

public class Dumper {
    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        String batchPath = null;
        if (args.length > 0) {
            batchPath = args[0];
        } else {
            System.err.println("Usage: java Segmenter <batch> [transform]");
            System.exit(-1);
        }

        if (args.length > 1) {
            String transformPath = args[1];
            System.err.println("Loading transform from... " + transformPath);
            SpeechTools.setTransform(transformPath);
        }

        for (String line : BatchFile.getLines(batchPath)) {
            System.out.println(BatchFile.getFilename(line));
            TranscriptAlignment t = SpeechTools.getTranscriptAlignment(line);
            for (FrameAlignment f : t.frames.values()) System.out.println(f);
            for (WordAlignment w : t.words) System.out.println(w);
            for (Segment s : t.getSegments()) System.out.println(s);
            System.out.println();

            t = null;
            System.gc();
        }

    }
}
