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
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class FrameAlignment {
    public final long time;
    public final WordAlignment wordAlignment;
    public final String word;
    public final Float logAcousticScore;
    public final String triphone;
    public final Integer stateId;
    public final Integer mId;
    public boolean isSpeech;
    public FloatData features;

    public FrameAlignment(Token token, WordAlignment wa) {
        this.time = token.getCollectTime();
        this.wordAlignment = wa;
        this.word = wa.word.toString();
        this.logAcousticScore = token.getAcousticScore();

        HMMState hmmState =
            ((HMMSearchState) token.getSearchState()).getHMMState();
        this.triphone = hmmState.getHMM().getUnit().toString();
        this.stateId = hmmState.getState();
        this.mId = (int) hmmState.getMixtureId();
        this.isSpeech = true;
    }

    public FrameAlignment(long time) {
        this.time = time;
        this.wordAlignment = null;
        this.word = null;
        this.logAcousticScore = null;
        this.triphone = null;
        this.stateId = null;
        this.mId = null;
        this.isSpeech = false;
    }

    public boolean isEmpty() {
        return !isSpeech && word == null;
    }

    public String toString() {
        return String.format("%5.2f %15s %12s %4s %12s %s",
                time / 1000.0,
                (word == null) ? " " : word,
                (triphone == null) ? " " : triphone,
                (stateId == null) ? " " : stateId,
                (logAcousticScore == null) ? " " : logAcousticScore,
                isSpeech);
    }
}
