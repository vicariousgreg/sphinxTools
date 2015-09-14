/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

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
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;

public class FrameResult {
    public final long time;
    public final long mId;
    public final int mId;
    public final Unit unit;
    public final Unit baseUnit;

    public final Data data;
    public final float score;

    public FrameResult(Token token) {
        HMMState hmmState = ((HMMSearchState) token.getSearchState()).getHMMState();
        HMM hmm = hmmState.getHMM();

        this.time = token.getCollectTime();
        this.mId = hmmState.getMixtureId();
        this.state = hmmState.getState();
        this.unit = hmm.getUnit();
        this.baseUnit = hmm.getBaseUnit();

        this.data = token.getData();
        this.score = token.getAcousticScore();
    }
}
