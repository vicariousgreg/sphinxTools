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
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class FrameAlignment {
    public final long time;
    public final String word;
    public final Float logAcousticScore;
    public final String triphone;
    public final Integer stateId;
    public boolean isSpeech;

    public FrameAlignment(Token token, String word) {
        this.time = token.getCollectTime();
        this.word = word;
        this.logAcousticScore = token.getAcousticScore();
        this.triphone = 
            ((HMMSearchState) token.getSearchState())
            .getHMMState().getHMM().getUnit().toString();
        this.stateId = 
            ((HMMSearchState) token.getSearchState())
            .getHMMState().getState();
        this.isSpeech = true;
    }

    public FrameAlignment(long time) {
        this.time = time;
        this.word = null;
        this.logAcousticScore = null;
        this.triphone = null;
        this.stateId = null;
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
