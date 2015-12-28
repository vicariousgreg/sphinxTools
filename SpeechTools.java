import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.adaptation.*;
import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.*;
import edu.cmu.sphinx.frontend.util.*;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.linguist.*;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.BatchFile;

public class SpeechTools {
    private static final String ACOUSTIC_MODEL_PATH =
            "/audio/models/transcription/acoustic/models/en-us";
    private static final String DICTIONARY_PATH =
            "/audio/models/transcription/dictionary/cmudict-en-us.dict";
            //"/audio/models/transcription/en_us_nostress/cmudict-5prealpha.dict";
    private static final String G2P_PATH =
            "/audio/models/transcription/en_us_nostress/model.fst.ser";
    private static Context context;
    private static StreamSpeechRecognizer recognizer;
    private static String transformPath;

    ///////////////////
    /* Sphinx tools. */
    ///////////////////

    /**
     * Gets the Context.
     * @return context
     */
    public static Context getContext() {
        if (context == null)
            initializeContext();
        return context;
    }

    public static void initializeContext() {
        Configuration config = new Configuration();
        config.setAcousticModelPath(ACOUSTIC_MODEL_PATH);
        config.setDictionaryPath(DICTIONARY_PATH);
        try {
            context = new Context(config);
            if (transformPath != null) {
                ClusteredDensityFileData clusters = new ClusteredDensityFileData(context.getLoader(), 1);
                Transform transform = new Transform((Sphinx3Loader)context.getLoader(), 1);
                transform.load(transformPath);
                context.getLoader().update(transform, clusters);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    public static Dictionary getDictionary() {
        Dictionary dictionary = null;
        try {
            dictionary = ((Dictionary) getContext().getConfigurationManager().lookup("dictionary"));
            dictionary.allocate();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
        return dictionary;
    }

    public static SpeechAligner getSpeechAligner() {
        SpeechAligner aligner = null;
        try {
            aligner = new SpeechAligner(ACOUSTIC_MODEL_PATH, DICTIONARY_PATH, G2P_PATH);
            aligner.setTokenizer(new USEnglishTokenizer());
            if (transformPath != null) aligner.setTransform(transformPath);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
        return aligner;
    }

    public static StreamSpeechRecognizer getRecognizer() {
        if (recognizer == null) {
            initializeRecognizer();
        }
        return recognizer;
    }

    public static void setTransform(String transformPath) throws Exception {
        SpeechTools.transformPath = transformPath;
        initializeRecognizer();
    }

    public static void initializeRecognizer() {
        try {
            Configuration configuration = new Configuration();

            configuration
                    .setAcousticModelPath("/audio/models/transcription/acoustic/models/en-us");
            configuration
                    .setLanguageModelPath("/audio/models/transcription/language/models/en-us.lm");
            configuration
                    .setDictionaryPath("/audio/models/transcription/dictionary/cmudict-en-us.dict");

            recognizer = new StreamSpeechRecognizer(configuration);
            if (transformPath != null) recognizer.loadTransform(transformPath, 1);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }


    /////////////////////////////////
    /* Low level extraction tools. */
    /////////////////////////////////

    /**
     * Extracts speech classified data from an input stream.
     * @param audioUrl url of audio file
     * @return list of speech classified data
     */
    public static List<SpeechClassifiedData> getSpeechClassifiedData(URL audioUrl) throws Exception {
        //System.out.println("Get speech data...");
        List<SpeechClassifiedData> out = new ArrayList<SpeechClassifiedData>();

        Context context = getContext();
        ConfigurationManager cm = context.getConfigurationManager();

        FrontEnd frontEnd = cm.lookup("speechFrontEnd");
        context.setSpeechSource(audioUrl.openStream());

        Data data = null;
        while ((data = frontEnd.getData()) != null) {
            if (data instanceof SpeechClassifiedData) {
                out.add((SpeechClassifiedData) data);
            }
        }
        return out;
    }


    /**
     * Extracts feature data from an input stream.
     * @param audioUrl url of audio file
     * @param classifySpeech whether to include speech classification
     * @return list of feature data
     */
    public static List<FloatData> getFeatures(URL audioUrl) throws Exception {
        //System.out.println("Get features...");
        List<FloatData> out = new ArrayList<FloatData>();

        Context context = getContext();
        ConfigurationManager cm = context.getConfigurationManager();

        FrontEnd frontEnd = cm.lookup("unmarkedFrontEnd");
        context.setSpeechSource(audioUrl.openStream());

        Data data = null;
        while ((data = frontEnd.getData()) != null) {
            if (data instanceof FloatData) {
                out.add((FloatData) data);
            }
        }
        return out;
    }


    //////////////////////////////
    /* Higher level processing. */
    //////////////////////////////

    /**
     * Creates a SenoneDump containing acoustic scores for each Senone for
     * each frame in the input stream.
     * @param audioUrl url of audio file
     * @return senone dump
     */
    public static SenoneDump getSenoneDump(URL audioUrl) throws Exception {
        SenoneDump dmp = new SenoneDump(getContext().getLoader());
        for (Data data : getFeatures(audioUrl)) {
            if (data instanceof FloatData) {
                dmp.addFrame(data);
            }
        }
        return dmp;
    }

    //////////////////////
    /* Alignment tools. */
    //////////////////////

    public static TranscriptAlignment getTranscriptAlignment(String batchLine) throws Exception {
        URL audioUrl = new File(BatchFile.getFilename(batchLine)).toURI().toURL();
        String transcript = BatchFile.getReference(batchLine);
        return getTranscriptAlignment(audioUrl, transcript);
    }

    public static TranscriptAlignment getTranscriptAlignment(URL audioUrl, URL transcriptUrl) throws Exception {
        // Load transcript
        Scanner scanner = new Scanner(transcriptUrl.openStream());  
        scanner.useDelimiter("\\Z");  
        String transcript = scanner.next();
        scanner.close();
        return getTranscriptAlignment(audioUrl, transcript);
    }

    public static TranscriptAlignment getTranscriptAlignment(URL audioUrl, String transcript) throws Exception {
        Context context = getContext();
        context.setLocalProperty("trivialScorer->frontend", "unmarkedFrontEnd");

        return new TranscriptAlignment(transcript,
                getWordAlignment(audioUrl, transcript), 
                getSpeechClassifiedData(audioUrl),
                getFeatures(audioUrl));
    }

    /**
     * Creates a word alignment given audio and a transcript.
     * @param audioUrl audio location
     * @param transcript transcript
     * @return word alignment list
     */
    public static List<WordResult> getWordAlignment(URL audioUrl, String transcript) throws Exception {
        //System.out.println("Get word alignment...");
        return getSpeechAligner().align(audioUrl, transcript);
    }

    //////////////////////////
    /* Transcription tools. */
    //////////////////////////

    public static String transcribe(URL audioUrl) throws Exception {
        initializeRecognizer();
        StreamSpeechRecognizer recognizer = getRecognizer();

        // Simple recognition with generic model
        InputStream stream = audioUrl.openStream();
        stream.skip(44);

        List<String> output = new ArrayList<String>();

        SpeechResult result;
        recognizer.startRecognition(stream);
        while ((result = recognizer.getResult()) != null) {
            output.add(result.getHypothesis());
            System.out.println(result.getHypothesis());
        }
        recognizer.stopRecognition();

        StringBuilder sb = new StringBuilder();
        if (output.size() > 0) {
            sb.append(output.get(0));
            int index = 1;
            while (index < output.size())
                sb.append(" " + output.get(index++));

        }
        return sb.toString();
    }
}
