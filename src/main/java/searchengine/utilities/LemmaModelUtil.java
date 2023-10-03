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
import java.util.Set;

@Service
public class LemmaModelUtil {

    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;

    public void createLemmaModel(SiteModel siteModel, PageModel pageModel) throws IOException {
//        PageModel pageForLemmas = pageRepository.findPageByPath(path);
        String pageForLemmasHtml = extractTextFromPageContent(pageModel.getContent());
//        String pageForLemmasHtml = extractText(pageForLemmas.getContent());
//        String pageForLemmasHtml = pageForLemmas.getContent();

        Map<String, Integer> lemmasCountByPage = lemmaFinderUtil.getLemmasMap(pageForLemmasHtml);
        Set<String> lemmasSet = lemmasCountByPage.keySet();
        for (String lemmaForPage : lemmasSet) {

            // !!! вероятно ошибка возникает в этом месте когда лемма есть в двух ID сайтов

            LemmaModel lemmaModel = lemmaRepository.findByLemma(lemmaForPage);
            if (lemmaModel != null) {
                int frequency = lemmaModel.getFrequency();
                lemmaModel.setFrequency(frequency + 1);
                lemmaRepository.save(lemmaModel);
            // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, lemmaModel, pageForLemmas);
                continue;
            }
            LemmaModel newLemmaModel = new LemmaModel();
            newLemmaModel.setFrequency(1);
            newLemmaModel.setLemma(lemmaForPage);
            newLemmaModel.setSiteId(siteModel);
            lemmaRepository.save(newLemmaModel);
            // saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, newLemmaModel, pageForLemmas);
        }
    }

    public static String extractTextFromPageContent(String html) {
        return Jsoup.parse(html).text();
    }
}
