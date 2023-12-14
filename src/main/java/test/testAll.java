package test;

import org.apache.commons.lang3.StringUtils;
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
import searchengine.model.PageModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SearchServiceImpl;
import searchengine.services.SiteIndexingServiceImpl;
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.ParserSiteUtil;
import searchengine.utilities.WordFinderUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

    @DisplayName("Тест получения сниппетов + выделение слов.")
    @Test
    void testSnippetWithSelectLemma() throws IOException {
        WordFinderUtil wordFinderUtil = null;
        try {
            wordFinderUtil = new WordFinderUtil();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fullContentPage = getResponse("https://art-kvartal.art").parse().outerHtml();
        List<String> requestList = Arrays.asList("беларусь", "интерьер", "для", "жизни");
        String onlyTextFromPage = wordFinderUtil.getTextFromFullContentPage(fullContentPage);
        log.info("ТОЛЬКО ТЕКСТ СО СТРАНИЦЫ : " + onlyTextFromPage);
        Map<String, Integer> snippetsOnPage = new HashMap<>();
        int finalSnippetSize = 200;
        for (String lemma : requestList) {
            Set<Integer> indexesLemmas = wordFinderUtil.getIndexesLemmaInText(onlyTextFromPage, lemma);
            log.info("Запрос-лемма - " + lemma + " - индекс леммы - " + indexesLemmas);
            log.info("Начало сбора сниппетов по одной лемме - " + lemma);
            for (int startIndex : indexesLemmas) {
                log.info("startIndex - " + startIndex);
                int endIndex = startIndex + finalSnippetSize / requestList.size();
                log.info("endIndex - " + endIndex);
                int nextSpaceIndex = onlyTextFromPage.indexOf(" ", endIndex);
                if (nextSpaceIndex != -1) {
                    endIndex = nextSpaceIndex;
                }
                String resultSnippet = onlyTextFromPage.substring(startIndex, endIndex).concat("...");
                log.info("РЕЗУЛЬТАТ. Сниппет по лемме - " + lemma + ", СНИППЕТ : " + resultSnippet);
                int countSearchLemmasOnPage = 0;
                countSearchLemmasOnPage += StringUtils.countMatches(onlyTextFromPage, lemma);
                snippetsOnPage.put(resultSnippet, countSearchLemmasOnPage);
                log.info("СНИППЕТЫ с одной леммой - " + startIndex + "__" + lemma + " - snippetsOnPage - " + snippetsOnPage);
            }
            log.info("ИТОГО список сниппетов по лемме " + lemma + " -- " + snippetsOnPage.keySet() + "\n");
        }
        log.info("СПИСОК НАЙДЕННЫХ СНИППЕТОВ ПО ВСЕМ ЛЕММАМ ИЗ ЗАПРОСА - " + snippetsOnPage.keySet());

        String newSnippets = snippetsOnPage.keySet().toString().toLowerCase();
        log.info("НОВЫЙ СПИСОК ВСЕХ СНИППЕТОВ в нижнем регистре - " + newSnippets);
        Queue<String> queueSnippets = new LinkedList<>();
        queueSnippets.add(newSnippets);
        requestList.forEach(searchLemma -> {
            String queueSnippet = queueSnippets.poll();
            assert queueSnippet != null;
            queueSnippets.add(queueSnippet.replaceAll(searchLemma, "<b> ".concat(searchLemma).concat(" </b>")));
        });
        log.info("РЕЗУЛЬТАТ - СПИСОК ВСЕХ СНИППЕТОВ С ВЫДЕЛЕННЫМИ СЛОВАМИ ЗАПРОСА : " + queueSnippets.poll());
    }

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

    @Test
    void testGetSnippet() throws IOException {
        WordFinderUtil wordFinderUtil = null;
        try {
            wordFinderUtil = new WordFinderUtil();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fullContentPage = getResponse("https://art-kvartal.art").parse().outerHtml();
        List<String> requestList = Arrays.asList("интерьер", "для", "жизни");
        log.info(wordFinderUtil.getSnippet(fullContentPage, requestList));
    }

    @Test
    void getTextSnippetWithSelectLemma() throws IOException {
        String newSortedSnippets = "Фура при опережении попутной автомашины выехала на разделительную полосу, " +
                "наехала на сугроб снега, ее занесло, после чего она вылетела на " +
                "встречку и врезалась в машину. К сожалению, без трагедий обойтись не удалось. " +
                "В результате ДТП, 45-летний житель Дзержинского района, водитель грузовика от полученных травм " +
                "скончался в больнице во вторник.";
        List<String> requestList = Arrays.asList("района", "выехала", "занесло", "травм");
        WordFinderUtil wordFinderUtil = null;
        try {
            wordFinderUtil = new WordFinderUtil();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info(wordFinderUtil.getTextSnippetWithSelectLemma(newSortedSnippets, requestList));
    }
}



