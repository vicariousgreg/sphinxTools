import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.USEnglishTokenizer;
import edu.cmu.sphinx.api.SpeechAligner;
import edu.cmu.sphinx.result.WordResult;

public class TokenizerDemo {
    public static void main(String args[]) throws Exception {
        String transcript = "";
        if (args.length > 0) {
            Scanner scanner = new Scanner(new File(args[0]));  
            scanner.useDelimiter("\\Z");  
            transcript = scanner.next();
            scanner.close();
        } else {
           USEnglishTokenizer tokenizer = new USEnglishTokenizer();
           Scanner in = new Scanner(System.in);
           while(true) {
              String input = in.nextLine();
              System.out.println("TOKENS:");
              for (String s : tokenizer.expand(input))
                  System.out.println(s);
              System.out.println();
           }
        }
        USEnglishTokenizer tokenizer = new USEnglishTokenizer();
        for (String s : tokenizer.expand(transcript)) {
            System.out.println(s);
        }
     }
}
