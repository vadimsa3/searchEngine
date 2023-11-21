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

/*  Сниппеты — фрагменты текстов, в которых найдены совпадения, для
    всех страниц должны быть примерно одинаковой длины — такие, чтобы
    на странице с результатами поиска они занимали примерно три строки.

    В них необходимо выделять жирным совпадения с исходным поисковым запросом.
    Выделение должно происходить в формате HTML при помощи тега <b>.
    Алгоритм получения сниппета из веб-страницы реализуйте самостоятельно.*/

    // result = result.replaceAll(word,"<b>" + word + "<b>"); ВЫДЕЛЯЕТ ВЕСЬ СНИППЕТ РЕАЛИЗОВАТЬ ВЫДЕЛЕНИЕ !!!!

    public String getSnippet(String fullContentPage, List<String> lemmas) {
        if (lemmas.isEmpty()) {
            return null;
        }
        String onlyTextPage = getTextFromFullContentPage(fullContentPage);
        List<Integer> indexInText = getFirstIndexInText(onlyTextPage, lemmas);
        int start = 0;
        int end = 0;
        int maxLemmas = 0;
        for (int startIndex : indexInText) {
            int endIndex = startIndex + 200; // сниппет в 3 строки
            int nextSpaceIndex = onlyTextPage.indexOf(" ", endIndex);
            if (nextSpaceIndex != -1) {
                endIndex = nextSpaceIndex;
            }
            int currentLemmas = 0;
            for (String lemma : lemmas) {
                if (onlyTextPage.toLowerCase().substring(startIndex, endIndex).contains(lemma)) {
                    currentLemmas++;
                }
            }
            if (currentLemmas > maxLemmas) {
                maxLemmas = currentLemmas;
                start = startIndex;
                end = endIndex;
            }
        }
        return onlyTextPage.substring(start, end);
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
        if (lemmaFinderUtil.anyWordBaseBelongToParticle(wordBaseForms)) {
            return false;
        }
        List<String> normalForms = lemmaFinderUtil.luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return false;
        }
        return normalForms.get(0).equals(lemma);
    }
}