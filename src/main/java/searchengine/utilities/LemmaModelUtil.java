package searchengine.utilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LemmaModelUtil {

    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;

    public void createLemmaModel(PageModel pageModel, SiteModel siteModel) throws IOException {
        String textPageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
        Map<String, Integer> lemmasCountByPage = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasCountByPage.keySet();
        for (String lemmaForPage : lemmasSet) {
            synchronized (lemmaRepository) { // для исключеня дублирования записи в репозиторий лемм другими потоками
                LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSiteId(lemmaForPage, siteModel);
                if (lemmaModel != null) {
                    lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
                    lemmaRepository.save(lemmaModel);
                    // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, lemmaModel, pageForLemmas);
                } else {
                    LemmaModel newLemmaModel = new LemmaModel();
                    newLemmaModel.setSiteId(siteModel);
                    newLemmaModel.setLemma(lemmaForPage);
                    newLemmaModel.setFrequency(1);
                    lemmaRepository.save(newLemmaModel);
                    // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, newLemmaModel, pageForLemmas);
                }
            }
        }
    }

    public static String extractTextFromPageContent(String html) {
        return Jsoup.parse(html).text();
    }
}