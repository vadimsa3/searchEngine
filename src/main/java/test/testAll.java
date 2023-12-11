package test;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.services.SearchServiceImpl;
import searchengine.services.SiteIndexingServiceImpl;
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.WordFinderUtil;

import java.io.IOException;
import java.util.*;

public class testAll {

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);

    @Autowired
    private SitesList sitesList;
    @Autowired
    private Site site;

    @DisplayName("Метод getLemmasMap. Получение из страницы лемм с количеством.")
    @Test
    void testGetLemmasMapFromUrl() throws IOException {
        String webPageUrl = "https://tortishnaya.ru";
        Connection.Response response = Jsoup.connect(webPageUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                .referrer("http://www.google.com")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .execute();
        Document document = response.parse();
        String text = document.text();
        LemmaFinderUtil lemmaFinderUtil = new LemmaFinderUtil();
        System.out.println(lemmaFinderUtil.getLemmasMap(text));
    }

    @Test
    void testSaveNewOrUpdateOldLemma() throws IOException {
        String webPageUrl = "https://tortishnaya.ru/cooperate";
        Connection.Response response = Jsoup.connect(webPageUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                .referrer("http://www.google.com")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .execute();
        Document doc = response.parse();
        String textPageForLemmasHtml = doc.text();
        LemmaFinderUtil lemmaFinderUtil = new LemmaFinderUtil();

        Map<String, Integer> lemmasMap = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        System.out.println("MAP " + lemmasMap);

        Set<String> lemmasSet = lemmasMap.keySet();
        for (String word : lemmasSet) {
            int countLemmaOnPage = lemmasMap.get(word);
            System.out.println(word + " - " + countLemmaOnPage);
        }
    }

    @Test
    void testCheckEnterWordLanguage() throws IOException {
        SearchServiceImpl searchService = new SearchServiceImpl();
        String queryRus = "Рецепт клубничного торта";
        String queryEng = "Рецепт Strawberry";
        String queryEmpty = "";
        System.out.println("QueryRus " + searchService.checkLanguageInputQuery(queryRus));
        System.out.println("QueryEng " + searchService.checkLanguageInputQuery(queryEng));
        System.out.println("QueryEmpty " + searchService.checkLanguageInputQuery(queryEmpty));
    }

    @Test
    void testText() throws IOException {
        String webPageUrl = "https://tortishnaya.ru/cooperate";
        Connection.Response response = Jsoup.connect(webPageUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                .referrer("http://www.google.com")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .execute();
        Document doc = response.parse();
        String textPageHtml = doc.text();
        log.info("textPage: " + textPageHtml);

        for (Element headline : doc.getElementsContainingText("начинка")) {
            log.info(headline.text());
        }
    }

    @Test
    void testGetTextSnippetWithSelectLemma () throws IOException {
        WordFinderUtil wordFinderUtil = null;
        try {
            wordFinderUtil = new WordFinderUtil();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        String pageContent = "Мы принимаем заказы на изготовление тортов за 5 дней. " +
//                "Для заказа можно воспользоваться калькулятором ниже. Также вы можете позвонить или написать " +
//                "нам в любом мессенджере, и мы поможем вам с выбором. Если вдруг вы не успеваете по дням, " +
//                "то всё равно напишите. Мы готовим торты для витрины и с удовольствием оформим один " +
//                "из них специально для вас.";

        String fullContentPage = getResponse("https://tortishnaya.ru/tort").parse().outerHtml();

        List<String> lemmas = new ArrayList<>();
        lemmas.add("торт");
        lemmas.add("витрина");
        lemmas.add("один");

        StringBuilder stringBuilder = new StringBuilder(200);

        String onlyTextFromPage = wordFinderUtil.getTextFromFullContentPage(fullContentPage);
System.out.println(onlyTextFromPage);

        Map<String, Integer> snippetsOnPage = new HashMap<>();

        for (String lemma : lemmas) {
            Set<Integer> indexesLemmas = wordFinderUtil.getIndexesLemmaInText(onlyTextFromPage, lemma);
System.out.println("lemma - " + lemma + " - indexes Lemmas - " + indexesLemmas);

            for (int startIndex : indexesLemmas) {
System.out.println("startIndex - " + startIndex);
                int endIndex = startIndex + 50;
System.out.println("endIndex - " + endIndex);
                int nextSpaceIndex = onlyTextFromPage.indexOf(" ", endIndex);
                if (nextSpaceIndex != -1) {
                    endIndex = nextSpaceIndex;
                }
                String resultSnippet = onlyTextFromPage.substring(startIndex, endIndex)
                        .concat("...");
System.out.println("resultSnippet for lemma - " + lemma + " - " + resultSnippet);
                int countSearchLemmasOnPage = 0;
                countSearchLemmasOnPage += StringUtils.countMatches(onlyTextFromPage, lemma);
                snippetsOnPage.put(resultSnippet, countSearchLemmasOnPage);
System.out.println("snippetsOnPage - " + snippetsOnPage);
            }

//            for (String snippet : snippetsOnPage.keySet()) {
//                stringBuilder.append(snippet.concat("..."));
//            }
//System.out.println("result : " + stringBuilder);
            stringBuilder.append(wordFinderUtil.getTextSnippetWithSelectLemma(snippetsOnPage.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null).getKey(), lemmas).concat("..."));
            System.out.println("result : " + stringBuilder);
        }


//        return snippetsOnPage.isEmpty()
//                ? null
//                : wordFinderUtil.getTextSnippetWithSelectLemma(snippetsOnPage.entrySet().stream()
//                .max(Map.Entry.comparingByValue()).orElse(null).getKey(), lemmas);
    }



//
//        ArrayList<String> cleanSplittedContent = cleanElements(pageContent.split("\n"));
//        System.out.println("cleanSplittedContent " + cleanSplittedContent);
//
//        List<String> wordsList = new ArrayList<>();
//        for (String textPart : cleanSplittedContent) {
//            for (String part : textPart.split(" ")) {
//                if (part.isEmpty() || !part.matches("[А-я]+")) {
//                    continue;
//                }
//                System.out.println(part);
//                wordsList.add(part);
//            }
//        }
//
//        LemmaFinderUtil lemmaFinderUtil = new LemmaFinderUtil();
//        Map<String, Integer> normalForm = lemmaFinderUtil.getLemmasMap(pageContent);
//        System.out.println("normalForm TEXT : " + normalForm);
//
//        for (String textPart : cleanSplittedContent) {
//            String tempTextPart = textPart;
//            for (String l : lemmas) {
//                if (textPart.contains(l)) {
//                    tempTextPart = tempTextPart.replaceAll("\\b" + l + "\\b",
//                                    "<b>" + l + "</b>")
//                            .replaceAll("\"", "'") + " ";
//                }
//            }
//            result.append((textPart.equals(tempTextPart)) ? "" : tempTextPart);
//            System.out.println(result);
//        }


    public Connection.Response getResponse(String link) throws IOException {
        return Jsoup.connect(link)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                .referrer("http://google.com")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .execute();
    }

//
//        StringBuilder result = new StringBuilder();
//        lemmas.forEach(lemma -> {
//            String textSnippetFromList = listTextSnippetWithLemmaSelect.get(0);
//            String textSnippetWithLemmaSelect = textSnippetFromList.toLowerCase()
//                    .replaceAll(lemma, "<b> ".concat(lemma).concat(" </b>"));
//            listTextSnippetWithLemmaSelect.add(0, textSnippetWithLemmaSelect);
//        });
//
//
//
//        System.out.println(wordFinderUtil.getTextSnippetWithSelectLemma(textSnippet, lemmas));
    }



