import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.DataOutputStream;

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

public class FeatureDumper {
    public static void main(String args[]) throws Exception {
        Context.setCustomConfig("/audio/tools/transcription/jar/config.xml");

        String batchPath = null;
        if (args.length > 0) {
            batchPath = args[0];
        } else {
            System.err.println("Usage: java FeatureDumper <batch>");
            System.exit(-1);
        }

        DataOutputStream os = new DataOutputStream(System.out);

        for (String line : BatchFile.getLines(batchPath)) {
            System.out.println(BatchFile.getFilename(line));
            TranscriptAlignment t = SpeechTools.getTranscriptAlignment(line);
            for (FrameAlignment f : t.frames.values()) {
                if (f.mId != null) {
                    StringBuilder sb = new StringBuilder(String.format("%d", f.mId));

                    os.writeInt(f.mId);

                    for (float val : f.features.getValues()) {
                        sb.append(String.format(" %f", val));
                        os.writeFloat(val);
                    }
                    //os.writeChar('\n');
                    //System.out.printf(sb.toString() + "\n");
                }
            }
            System.out.println();

            t = null;
            System.gc();
        }

    }
}
