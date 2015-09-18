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
import edu.cmu.sphinx.util.NISTAlign;

public class Evaluator {
    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        String batchPath = null;
        String transformPath = null;
        if (args.length > 0) {
            batchPath = args[0];
        } else {
            System.err.println("Usage: java Evaluator <batch> [transform]");
            System.exit(-1);
        }

        if (args.length > 1) {
            transformPath = args[1];
            System.out.println("Loading transform from... " + transformPath);
        }

        NISTAlign aligner = new NISTAlign(true, true);

        for (String line : BatchFile.getLines(batchPath)) {
            System.out.println("Processing... " + BatchFile.getFilename(line));
            URL audioUrl = new File(BatchFile.getFilename(line)).toURI().toURL();
            String transcript = BatchFile.getReference(line);

            StringBuilder sb = new StringBuilder();
            for (String word : new USEnglishTokenizer().expand(transcript)) {
                sb.append(word + " ");
            }
            transcript = sb.toString();

            String hypothesis = SpeechTools.transcribe(audioUrl, transformPath);
            aligner.align(transcript, hypothesis);
            System.gc();
        }
        aligner.printTotalSummary();
    }
}
