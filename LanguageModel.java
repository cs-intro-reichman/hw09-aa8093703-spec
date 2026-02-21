import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
    private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     *  seed value. Generating texts from this model multiple times with the 
     *  same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        CharDataMap.clear();
        In in = new In(fileName);
        String text = in.readAll();
        in.close();

        if (windowLength <= 0 || text.length() <= windowLength) {
            return;
        }

        for (int i = 0; i + windowLength < text.length(); i++) {
            String window = text.substring(i, i + windowLength);
            char nextChar = text.charAt(i + windowLength);

            List probs = CharDataMap.get(window);
            if (probs == null) {
                probs = new List();
                CharDataMap.put(window, probs);
            }
            probs.update(nextChar);
        }

        for (String key : CharDataMap.keySet()) {
            calculateProbabilities(CharDataMap.get(key));
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    void calculateProbabilities(List probs) {
        if (probs == null || probs.getSize() == 0) {
            return;
        }

        int totalCount = 0;
        ListIterator it = probs.listIterator(0);
        while (it != null && it.hasNext()) {
            totalCount += it.next().count;
        }

        double cumulative = 0.0;
        it = probs.listIterator(0);
        while (it != null && it.hasNext()) {
            CharData cd = it.next();
            cd.p = (double) cd.count / totalCount;
            cumulative += cd.p;
            cd.cp = cumulative;
        }
    }

    // Returns a random character from the given probabilities list.
    char getRandomChar(List probs) {
        if (probs == null || probs.getSize() == 0) {
            return ' ';
        }

        double randomValue = randomGenerator.nextDouble();
        ListIterator it = probs.listIterator(0);
        CharData last = probs.getFirst();

        while (it != null && it.hasNext()) {
            CharData cd = it.next();
            last = cd;
            if (randomValue <= cd.cp) {
                return cd.chr;
            }
        }

        return last.chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during training. 
     * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
     * doesn't appear as a key in Map, we generate no text and return only the initial text. 
     * @param numberOfLetters - the size of text to generate
     * @return the generated text
     */
    public String generate(String initialText, int textLength) {
        if (initialText == null) {
            return null;
        }
        if (windowLength <= 0 || textLength <= 0) {
            return initialText;
        }

        StringBuilder generatedText = new StringBuilder(initialText);
        for (int i = 0; i < textLength; i++) {
            if (generatedText.length() < windowLength) {
                break;
            }

            String window = generatedText.substring(generatedText.length() - windowLength);
            List probs = CharDataMap.get(window);
            if (probs == null || probs.getSize() == 0) {
                break;
            }

            generatedText.append(getRandomChar(probs));
        }

        return generatedText.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java LanguageModel <windowLength> <initialText> <textLength> <fixed/random> <fileName>");
            return;
        }

        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int textLength = Integer.parseInt(args[2]);
        boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];

        LanguageModel model;
        if (randomGeneration) {
            model = new LanguageModel(windowLength);
        } else {
            model = new LanguageModel(windowLength, 20);
        }

        model.train(fileName);
        System.out.println(model.generate(initialText, textLength));
    }
}
