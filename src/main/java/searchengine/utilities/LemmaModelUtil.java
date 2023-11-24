package searchengine.utilities;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class LemmaModelUtil {

    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;
    @Autowired
    private IndexModelUtil indexModelUtil;

    public void createNewLemmaModel(PageModel pageModel, SiteModel siteModel) throws IOException {
        String textPageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
        Map<String, Integer> lemmasAndCountByPage = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasAndCountByPage.keySet();
        for (String lemmaForPage : lemmasSet) {
            synchronized (lemmaRepository) {
                LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSiteId(lemmaForPage, siteModel);
                int countLemmaOnPage = lemmasAndCountByPage.get(lemmaForPage);
                if (lemmaModel != null) {
                    int countLemmaOnSite = lemmaModel.getFrequency() + 1;
                    lemmaModel.setFrequency(countLemmaOnSite);
                    lemmaRepository.save(lemmaModel);
                    indexModelUtil.createIndexModel(pageModel, lemmaModel, countLemmaOnPage);
                } else {
                    LemmaModel newLemmaModel = new LemmaModel();
                    newLemmaModel.setSiteId(siteModel);
                    newLemmaModel.setLemma(lemmaForPage);
                    newLemmaModel.setFrequency(1);
                    lemmaRepository.save(newLemmaModel);
                    indexModelUtil.createIndexModel(pageModel, newLemmaModel, countLemmaOnPage);
                }
            }
        }
    }

    // Amount не правильно пишется - выдает кол-во страниц по порядку, а не кол-во лемм на странице

    public static String extractTextFromPageContent(String html) {
        return Jsoup.parse(html).text();
    }
}