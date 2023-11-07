package searchengine.utilities;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class WordFinderUtil {

    private final LemmaFinderUtil lemmaFinderUtil;

    public WordFinderUtil() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
    }

    // !!!!!ПРОВЕРИТЬ ГЛЮК СО СНИППЕТАМИ !!!!
        public String getMatchingSnippet(String fullText, List<String> lemmas) throws IOException {
        String text = clearTagOnHtml(fullText);
        System.out.println(lemmas);
        String newText = text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        String[] words = newText.split("\\s");

        List<Integer> indexInText = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            for (String lemma : lemmas) {
                if (isLemmaInText(lemma, word)) {
                    indexInText.add(text.toLowerCase().indexOf(word));
                }
            }
        }

        Collections.sort(indexInText);

        int bestStartIndex = 0;
        int bestEndIndex = 0;
        int maxLemmas = 0;

        for (int i = 0; i < indexInText.size(); i++) {
            int startIndex = indexInText.get(i);
            int endIndex = startIndex + 150;
            int nextSpaceIndex = text.indexOf(" ", endIndex);
            if (nextSpaceIndex != -1) {
                endIndex = nextSpaceIndex;
            }

            int currentLemmas = 0;
            for (String lemma : lemmas) {
                if (text.toLowerCase().substring(startIndex, endIndex).contains(lemma)) {
                    currentLemmas++;
                }
            }
            if (currentLemmas > maxLemmas) {
                maxLemmas = currentLemmas;
                bestStartIndex = startIndex;
                bestEndIndex = endIndex;
            }
        }

        return text.substring(bestStartIndex, bestEndIndex);
    }

    public String getTitle(String html) {
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        return document.title();
    }

    private String clearTagOnHtml(String html) {
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        return document.title();
    }

    public boolean isLemmaInText(String lemma, String word) {
        if (word.isBlank()) {
            return false;
        }
        List<String> wordBaseForms = lemmaFinderUtil.luceneMorphology.getMorphInfo(word);
        if (lemmaFinderUtil.anyWordBaseBelongToParticle(wordBaseForms)) {
            return false;
        }
        List<String> normalForms = lemmaFinderUtil.luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return false;
        }
        return normalForms.get(0).equals(lemma);
    }

    // на всякий случай
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
