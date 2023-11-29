package searchengine.utilities;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WordFinderUtil {

    private final LemmaFinderUtil lemmaFinderUtil;

    public WordFinderUtil() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
    }

    public List<String> getSnippet(String fullContentPage, List<String> lemmas) {
        if (lemmas.isEmpty()) {
            return null;
        }
        String onlyTextFromPage = getTextFromFullContentPage(fullContentPage);
        List<Integer> indexesLemmasInText = getFirstIndexInText(onlyTextFromPage, lemmas);
        int startSnippet = 0;
        int endSnippet = 0;
        int maxLemmas = 0;
        for (int startIndex : indexesLemmasInText) {
            int endIndex = startIndex + 200;
            int nextSpaceIndex = onlyTextFromPage.indexOf(" ", endIndex);
            if (nextSpaceIndex != -1) {
                endIndex = nextSpaceIndex;
            }
            int currentLemmas = 0;
            for (String lemma : lemmas) {
                if (onlyTextFromPage.toLowerCase().substring(startIndex, endIndex).contains(lemma)) {
                    currentLemmas++;
                }
            }
            if (currentLemmas > maxLemmas) {
                maxLemmas = currentLemmas;
                startSnippet = startIndex;
                endSnippet = endIndex;
            }
        }
        String textSnippet = onlyTextFromPage.substring(startSnippet, endSnippet);
        return getTextSnippetWithSelectLemma(textSnippet, lemmas);
    }

    public List<Integer> getFirstIndexInText(String onlyTextPage, List<String> lemmas) {
        String newEditText = onlyTextPage.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        String[] onlyWordsFromText = newEditText.split("\\s");
        List<Integer> indexWordInText = new ArrayList<>();
        for (String word : onlyWordsFromText) {
            for (String lemma : lemmas) {
                if (isLemmaInText(lemma, word)) {
                    indexWordInText.add(onlyTextPage.toLowerCase().indexOf(word));
                }
            }
        }
        return indexWordInText;
    }

    public String getTitleFromFullContentPage(String html) {
        Document document = Jsoup.parse(html);
        return document.title();
    }

    private String getTextFromFullContentPage(String html) {
        Document document = Jsoup.parse(html);
        return document.text();
    }

    public boolean isLemmaInText(String lemma, String word) {
        if (word.isBlank()) {
            return false;
        }
        List<String> wordBaseForms = lemmaFinderUtil.luceneMorphology.getMorphInfo(word);
        if (lemmaFinderUtil.anyWordBaseFormBelongToParticle(wordBaseForms)) {
            return false;
        }
        List<String> normalWordForms = lemmaFinderUtil.luceneMorphology.getNormalForms(word);
        if (normalWordForms.isEmpty()) {
            return false;
        }
        return normalWordForms.get(0).equals(lemma);
    }

    private List<String> getTextSnippetWithSelectLemma(String textSnippet, List<String> lemmas) {
        List<String> result = new ArrayList<>();
        lemmas.forEach(lemma -> {
            String textSnippetWithLemmaSelect = textSnippet.toLowerCase()
                    .replaceAll(lemma, "<b> ".concat(lemma).concat(" </b>"));
            result.add(textSnippetWithLemmaSelect);
        });
        return result;
    }
}