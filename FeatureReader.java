import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import java.io.IOException;

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

public class FeatureReader {
    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        String featureFile = null;
        if (args.length > 0) {
            featureFile = args[0];
        } else {
            System.err.println("Usage: java FeatureReader <feature dump>");
            System.exit(-1);
        }

        for (LabelledFeature f : loadFeatures(featureFile)) {
            System.out.println(f);
        }
    }

    public static List<LabelledFeature> loadFeatures(String fileName) throws IOException {
        DataInputStream is = new DataInputStream(new FileInputStream(new File(fileName)));
        List<LabelledFeature> labelledFeatures = new ArrayList<LabelledFeature>();

        while (true) {
            try {
                int mId = is.readInt();
                List<Float> features = new ArrayList<Float>();
                float curr = 0.0f;
                
                for (int index = 0; index < 36; ++index) {
                    features.add(is.readFloat());
                }

                labelledFeatures.add(new LabelledFeature(mId, features));
            } catch (EOFException e) {
                break;
            }
        }
        return labelledFeatures;
    }

    public static class LabelledFeature {
        public final int mId;
        public final FloatData data;

        public LabelledFeature(int mId, List<Float> features) {
            this.mId = mId;

            float[] arr = new float[features.size()];
            int i = 0;
            for (Float val : features) {
                arr[i++] = val;
            }

            this.data = new FloatData(arr, 16000, 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(String.format("%d", this.mId));
            for (float val : this.data.getValues()) {
                sb.append(String.format(" %f", val));
            }
            return sb.toString();
        }
    }
}
