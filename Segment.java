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
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

public class Segment {
    public TimeFrame left;
    public TimeFrame right;
    public TimeFrame time;
    public List<WordAlignment> words;

    public Segment(TimeFrame left, TimeFrame time, TimeFrame right) {
        this.left = left;
        this.time = time;
        this.right = right;
        this.words = new ArrayList<WordAlignment>();
    }

    public long getStart() {
        return time.getStart();
    }

    public long getEnd() {
        return time.getEnd();
    }

    public long getContextStart() {
        return (left == null) ? time.getStart() : left.getStart();
    }

    public long getContextEnd() {
        return (right == null) ? time.getEnd() : right.getEnd();
    }

    public long getLength() {
        return getEnd() - getStart();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (words.size() > 0) sb.append(words.get(0).word.toString());
        for (int i = 1; i < words.size(); ++i)
            sb.append(" " + words.get(i).word.toString());

        return String.format("%f %f %s",
                getContextStart() / 1000.0, getContextEnd() / 1000.0, sb.toString());
    }

    public static List<Segment> merge(List<Segment> segments, long threshold) {
        List<Segment> mergedSegments = new ArrayList<Segment>();

        if (segments.size() < 2) return segments;

        for (int i = 0; i < segments.size(); ++i) {
            Segment curr = segments.get(i);
            //System.out.println("  " + curr);

            if (curr.words.size() == 0 ||
                    curr.getEnd() - curr.getStart() < threshold) {
                Segment left = (mergedSegments.size() > 0) ?
                    mergedSegments.get(mergedSegments.size()-1) : null;
                Segment right = (i < segments.size() - 1) ?
                    segments.get(i+1) : null;

                boolean useRight = (left == null);
                if (left != null && right != null)
                    useRight = (left.getLength() > right.getLength());

                if (useRight == true) {
                    Segment merged = merge(curr, right);

                    // Skip
                    ++i;
                    mergedSegments.add(merged);
                } else {
                    // use left
                    Segment merged = merge(left, curr);

                    // Replace
                    mergedSegments.remove(mergedSegments.size()-1);
                    mergedSegments.add(merged);
                }
            } else {
                mergedSegments.add(curr);
            }
        }
        return mergedSegments;
    }

    public static Segment merge(Segment leftSegment, Segment rightSegment) {
        Segment merged = new Segment(leftSegment.left,
                new TimeFrame(leftSegment.time.getStart(), rightSegment.time.getEnd()),
                rightSegment.right);

        // Add words
        for (WordAlignment wr : leftSegment.words) merged.words.add(wr);
        for (WordAlignment wr : rightSegment.words) merged.words.add(wr);

        return merged;
    }
}
