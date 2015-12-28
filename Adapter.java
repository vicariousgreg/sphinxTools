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
            System.err.println("Usage: java Adapter <batch> [transformPath]");
            System.exit(-1);
        }

        String transformPath = "test.transform";
        if (args.length > 1) {
            transformPath = args[1];
        }

        ClusteredDensityFileData clusters =
            new ClusteredDensityFileData(SpeechTools.getContext().getLoader(), 1);
        Stats stats =
            new Stats(SpeechTools.getContext().getLoader(), clusters);

        for (String line : BatchFile.getLines(batchPath)) {
            String fileName = BatchFile.getFilename(line);
            System.out.println(fileName);

            String featName = fileName.substring(0, fileName.lastIndexOf('.')) + ".feat";

            List<FloatData> features = new ArrayList<FloatData>();
            List<Integer> mids = new ArrayList<Integer>();

            //TranscriptAlignment t = SpeechTools.getTranscriptAlignment(line);
            for (FeatureReader.LabelledFeature f : FeatureReader.loadFeatures(featName)) {
                //System.out.println(f);
                features.add(f.data);
                mids.add(f.mId);
            }

            /*
            for (FrameAlignment f : t.frames.values()) {
                if (f.features != null && f.mId != null) {
                    features.add(f.features);
                    mids.add(f.mId);

                    System.out.printf("%d %d\n", f.time, f.mId);
                }
            }
            */
            stats.collect(features, mids);
            //t = null;
            //System.gc();
        }

        Transform transform = stats.createTransform();

        if (transform == null) {
            System.out.println("Not enough data for transform!");
        } else {
            transform.store(transformPath, 0);
            System.out.println("Writing transform to " + transformPath);
        }

    }
}
