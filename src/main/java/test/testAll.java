package test;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.repositories.LemmaRepository;
import searchengine.services.SearchServiceImpl;
import searchengine.utilities.LemmaFinderUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class testAll {

    LemmaRepository lemmaRepository;

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
            System.out.println(word + " - " +  countLemmaOnPage);
        }
    }

    @Test
    void testCheckEnterWordLanguage() throws IOException {
        SearchServiceImpl searchService = new SearchServiceImpl();
        String queryRus = "Рецепт клубничного торта";
        String queryEng = "Рецепт Strawberry";
        String queryEmpty = "";
        System.out.println("QueryRus " + searchService.checkEnterQueryLanguage(queryRus));
        System.out.println("QueryEng " + searchService.checkEnterQueryLanguage(queryEng));
        System.out.println("QueryEmpty " + searchService.checkEnterQueryLanguage(queryEmpty));
    }
}
