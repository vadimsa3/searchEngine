package searchengine.utilities;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.services.SearchServiceImpl;

import java.io.IOException;
import java.util.*;

@Service
public class WordFinderUtil {

    private final LemmaFinderUtil lemmaFinderUtil;

    public WordFinderUtil() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
    }

    Queue<String> queueSnippets = new LinkedList<>();

    public String getSnippet(String fullContentPage, List<String> requestList) {
        if (requestList.isEmpty()) {
            return null;
        }
        String onlyTextFromPage = getTextFromFullContentPage(fullContentPage);
        Map<String, Integer> snippetsOnPage = new HashMap<>();
        for (String lemma : requestList) {
            Set<Integer> indexesLemmas = getIndexesLemmaInText(onlyTextFromPage, lemma);
            for (int startIndex : indexesLemmas) {
                int endIndex = startIndex + 200;
                int nextSpaceIndex = onlyTextFromPage.indexOf(" ", endIndex);
                if (nextSpaceIndex != -1) {
                    endIndex = nextSpaceIndex;
                }
                String resultSnippet = onlyTextFromPage.substring(startIndex, endIndex)
                        .concat("...");
                int countSearchLemmasOnPage = 0;
                countSearchLemmasOnPage += StringUtils.countMatches(onlyTextFromPage, lemma);
                snippetsOnPage.put(resultSnippet, countSearchLemmasOnPage);
            }
        }
        return snippetsOnPage.isEmpty()
                ? null
                : getTextSnippetWithSelectLemma(snippetsOnPage.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null).getKey().toLowerCase(), requestList);
    }

    public Set<Integer> getIndexesLemmaInText(String onlyTextPage, String lemma) {
        String newEditText = onlyTextPage.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        String[] onlyWordsFromText = newEditText.split("\\s");
        Set<Integer> indexWordInText = new HashSet<>();
        for (String word : onlyWordsFromText) {
            if (isLemmaInText(lemma, word)) {
                indexWordInText.add(onlyTextPage.toLowerCase().indexOf(word));
            }
        }
        return indexWordInText;
    }

    public String getTitleFromFullContentPage(String html) {
        Document document = Jsoup.parse(html);
        return document.title();
    }

    public String getTextFromFullContentPage(String html) {
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

    public String getTextSnippetWithSelectLemma(String newSortedSnippets, List<String> requestList) {
        queueSnippets.clear();
        queueSnippets.add(newSortedSnippets);
        requestList.forEach(searchLemma -> {
            String queueSnippet = queueSnippets.poll();
            assert queueSnippet != null;
            queueSnippets.add(queueSnippet.replaceAll(searchLemma, "<b> ".concat(searchLemma).concat(" </b>")));
        });
        return queueSnippets.poll();
    }
}