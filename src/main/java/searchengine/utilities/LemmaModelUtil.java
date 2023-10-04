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

//    public void createLemmaModel(SiteModel siteModel, PageModel pageModel) throws IOException {
//        String textPageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
//        Map<String, Integer> lemmasMapByPage = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
//        Set<String> lemmasSet = lemmasMapByPage.keySet();
//        for (String lemmaForPage : lemmasSet) {
//            int countLemmas = lemmasMapByPage.get(lemmaForPage);
//            Optional<LemmaModel> existingLemmaOpt = lemmaRepository.findByLemma(lemmaForPage);
////            Optional<LemmaModel> existingLemmaOpt = lemmaRepository.findByLemmaAndSiteId(lemmaForPage, siteModel.getId());
//
//            if (existingLemmaOpt.isPresent()) {
//                LemmaModel existingLemma = existingLemmaOpt.get();
//                int count = existingLemma.getFrequency() + countLemmas;
//                existingLemma.setFrequency(count);
//                lemmaRepository.save(existingLemma);
////                saveSearchIndex(page, existingLemma, count);
//            } else {
//                LemmaModel newLemmaModel = new LemmaModel();
//                newLemmaModel.setSiteId(siteModel);
//                newLemmaModel.setFrequency(countLemmas);
//                newLemmaModel.setLemma(lemmaForPage);
//                lemmaRepository.save(newLemmaModel);
////                saveSearchIndex(page, newLemmaModel, countLemma);
//            }
//        }
//    }

    public void createLemmaModel(PageModel pageModel) throws IOException {
        String textPageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
        Map<String, Integer> lemmasCountByPage = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasCountByPage.keySet();
        for (String lemmaForPage : lemmasSet) {
//            LemmaModel lemmaModel = lemmaRepository.findByLemma(lemmaForPage);
            synchronized (lemmaRepository) { // для исключеня дублирования записи в репозиторий лемм другими потоками
                LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSiteId(lemmaForPage, pageModel.getSiteId().getId());
                if (lemmaModel != null) {
                    lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
                    // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, lemmaModel, pageForLemmas);
                } else {
                    lemmaModel.setSiteId(pageModel.getSiteId());
                    lemmaModel.setLemma(lemmaForPage);
                    lemmaModel.setFrequency(1);
                    // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, newLemmaModel, pageForLemmas);
                }
                lemmaRepository.save(lemmaModel);
            }
        }
    }

    public static String extractTextFromPageContent(String html) {
        return Jsoup.parse(html).text();
    }
}