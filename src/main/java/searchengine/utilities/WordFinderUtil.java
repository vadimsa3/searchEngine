package searchengine.utilities;

import org.apache.lucene.document.Document;
import org.jsoup.Jsoup;
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

/*    Сниппеты — фрагменты текстов, в которых найдены совпадения, для
    всех страниц должны быть примерно одинаковой длины — такие, чтобы
    на странице с результатами поиска они занимали примерно три строки.

    В них необходимо выделять жирным совпадения с исходным поисковым
    запросом.
    Выделение должно происходить в формате HTML при помощи
    тега <b>.
    Алгоритм получения сниппета из веб-страницы реализуйте
    самостоятельно.*/

    // !!!!!ПРОВЕРИТЬ ГЛЮК СО СНИППЕТАМИ - выдает один и тот-же, расширить до 3-х строк !!!!
    // result = result.replaceAll(word,"<b>" + word + "<b>"); РЕАЛИЗОВАТЬ ВЫДЕЛЕНИЕ !!!!

    public String getSnippet(String fullContentPage, List<String> lemmas) {
        if (lemmas.isEmpty()) { return null; }
        String onlyTextPage = getTextFromFullContentPage(fullContentPage);
        System.out.println("+++++++++++++ ПРОВЕРКА onlyTextPage из getSnippet " + onlyTextPage); // удалить
        System.out.println("+++++++++++++ ПРОВЕРКА передачи списка лемм в getSnippet " + lemmas); // удалить
        String newEditText = onlyTextPage.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        System.out.println("+++++++++++++ ПРОВЕРКА newText из getSnippet " + newEditText); // удалить
        String[] onlyWordsFromText = newEditText.split("\\s");
        System.out.println("+++++++++++++ ПРОВЕРКА onlyWordsFromText из getSnippet " + onlyWordsFromText.toString()); // удалить


        List<Integer> indexInText = new ArrayList<>();
        for (String word : onlyWordsFromText) {
            for (String lemma : lemmas) {
                if (isLemmaInText(lemma, word)) {
                    indexInText.add(onlyTextPage.toLowerCase().indexOf(word));
                }
            }
        }

        Collections.sort(indexInText);
        int bestStartIndex = 0;
        int bestEndIndex = 0;
        int maxLemmas = 0;
        for (int startIndex : indexInText) {
            int endIndex = startIndex + 200;
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
                bestStartIndex = startIndex;
                bestEndIndex = endIndex;
            }
        }
        return onlyTextPage.substring(bestStartIndex, bestEndIndex);
    }

    public String getTitleFromFullContentPage(String html) {
        return Jsoup.parse(html).title();
    }

    private String getTextFromFullContentPage(String html) {
        return Jsoup.parse(html).text();
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

    // !!! РЕАЛИЗОВАТЬ - проверка на язык ввода слова
    private String checkEnterWordLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-z]+";
        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "Необходимо ввести запрос на русском языке";
        } else {
            return "";
        }
    }
}
