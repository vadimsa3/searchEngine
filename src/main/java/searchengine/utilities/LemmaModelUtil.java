package searchengine.utilities;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;

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

    public void createNewLemmaModel(PageModel pageModel, SiteModel siteModel) {
        String textPageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
        Map<String, Integer> lemmasAndCountByPage = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasAndCountByPage.keySet();
        for (String lemmaFromPage : lemmasSet) {
            synchronized (lemmaRepository) {
                LemmaModel existLemmaModel = lemmaRepository.findByLemmaAndSiteId(lemmaFromPage, siteModel);
                int countLemmaOnPage = lemmasAndCountByPage.get(lemmaFromPage);
                if (existLemmaModel != null) {
                    int countLemmaOnSite = existLemmaModel.getFrequency() + 1;
                    existLemmaModel.setFrequency(countLemmaOnSite);
                    lemmaRepository.save(existLemmaModel);
                    indexModelUtil.createIndexModel(pageModel, existLemmaModel, countLemmaOnPage);
                } else {
                    LemmaModel newLemmaModel = new LemmaModel();
                    newLemmaModel.setSiteId(siteModel);
                    newLemmaModel.setLemma(lemmaFromPage);
                    newLemmaModel.setFrequency(1);
                    lemmaRepository.save(newLemmaModel);
                    indexModelUtil.createIndexModel(pageModel, newLemmaModel, countLemmaOnPage);
                }
            }
        }
    }

    public static String extractTextFromPageContent(String html) {
        return Jsoup.parse(html).text();
    }
}