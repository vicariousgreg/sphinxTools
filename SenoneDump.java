import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.LinkedHashMap;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;

public class SenoneDump {
    // Map of frames to senones to scores.
    private Map<Long, Map<Integer, Float>> scores;
    private final Pool<Senone> senonePool;

    public SenoneDump(Loader loader) {
        this.scores = new LinkedHashMap<Long, Map<Integer, Float>>();
        this.senonePool = loader.getSenonePool();
    }

    public void addFrame(Data data) {
        if (data instanceof FloatData) {
            FloatData fd = (FloatData) data;
            long time = fd.getCollectTime();
            System.out.println("Adding data... " + time);

            Map<Integer, Float> frameMap = new LinkedHashMap<Integer, Float>();
            scores.put(time, frameMap);

            for (int i = 0; i < senonePool.size(); ++i) {
                frameMap.put(i, senonePool.get(i).getScore(fd));
            }
        } else {
            throw new RuntimeException("Cannot score non-float data!");
        }
    }

    public void print() {
        System.out.println("Frames: " + scores.size());
        System.out.println("Senones: " + senonePool.size());
    }
}
