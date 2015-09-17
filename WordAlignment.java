import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.dictionary.*;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class WordAlignment {
    public final String word;
    public TimeFrame time;
    public final List<FrameAlignment> frames;

    public WordAlignment(String word, WordResult wr) {
        this.word = word;
        this.frames = new ArrayList<FrameAlignment>();

        if (wr != null) {
            if (wr.getTokens().size() == 0) {
                this.time = wr.getTimeFrame();
            } else {
                long start = wr.getTokens().get(0).getCollectTime();
                long end = wr.getTokens().get(wr.getTokens().size()-1).getCollectTime();
                this.time = new TimeFrame(start, end);

                for (Token token : wr.getTokens()) {
                    //this.frames.add(new FrameAlignment(token, wr.getWord().toString()));
                    this.frames.add(new FrameAlignment(token, this));
                }
            }
        } else {
            this.time = null;
        }
    }

    public String toString() {
        String start = (time == null) ? "" : Long.toString(time.getStart());
        String end = (time == null) ? "" : Long.toString(time.getEnd());
        return String.format("%10s %10s %20s", start, end, word);
    }
}
