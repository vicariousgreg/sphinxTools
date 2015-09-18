import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;
import edu.cmu.sphinx.util.BatchFile;

public class Adapter {
    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        String batchPath = null;
        if (args.length > 0) {
            batchPath = args[0];
        } else {
            System.err.println("Usage: java Segmenter <batch>");
            System.exit(-1);
        }

        for (String line : BatchFile.getLines(batchPath)) {
            System.out.println(BatchFile.getFilename(line));
            TranscriptAlignment t = SpeechTools.getTranscriptAlignment(line);

            List<FloatData> features = new ArrayList<FloatData>();
            List<Integer> mids = new ArrayList<Integer>();

            ClusteredDensityFileData clusters =
                new ClusteredDensityFileData(SpeechTools.getContext().getLoader(), 1);
            Stats stats =
                new Stats(SpeechTools.getContext().getLoader(), clusters);


            for (FrameAlignment f : t.frames.values()) {
                if (f.features != null && f.mId != null) {
                    features.add(f.features);
                    mids.add(f.mId);

                    System.out.printf("%d %d\n", f.time, f.mId);
                }
            }

            stats.collect(features, mids);
            Transform transform = stats.createTransform();

            if (transform == null) {
                System.out.println("Not enough data for transform!");
            } else {
                transform.store("test.transform", 0);
                System.out.println("Writing transform to test.transform");
            }

            t = null;
            System.gc();
        }

    }
}
