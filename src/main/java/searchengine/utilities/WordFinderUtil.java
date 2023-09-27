package searchengine.utilities;

public class WordFinderUtil {


    private String checkEnterWordLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-z]+";
        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "Enter a word in Russian";
        } else {
            return "";
        }
    }
}
