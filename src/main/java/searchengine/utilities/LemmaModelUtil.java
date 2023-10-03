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

    public void createLemmaModel(SiteModel siteModel, String path) throws IOException {
        PageModel pageForLemmas = pageRepository.findPageByPath(path);
        String pageForLemmasHtml = extractText(pageForLemmas.getContent());
//        String pageForLemmasHtml = pageForLemmas.getContent();

        Map<String, Integer> lemmasCountByPage = lemmaFinderUtil.getLemmasMap(pageForLemmasHtml);
        Set<String> lemmasSet = lemmasCountByPage.keySet();
        for (String lemmaForPage : lemmasSet) {
            LemmaModel lemmaModel = lemmaRepository.findByLemma(lemmaForPage);
            if (lemmaModel != null) {
                int frequency = lemmaModel.getFrequency();
                lemmaModel.setFrequency(frequency + 1);
                lemmaRepository.save(lemmaModel);
//                saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, lemmaModel, pageForLemmas);
                continue;
            }
            LemmaModel lemmaModel1 = new LemmaModel();
            lemmaModel1.setFrequency(1);
            lemmaModel1.setLemma(lemmaForPage);
            lemmaModel1.setSiteId(siteModel);
            lemmaRepository.save(lemmaModel1);
            //                saveSearchIndexInSearchIndexRepository(lemmasCountByPage, lemmaForPage, lemmaModel1, pageForLemmas);
        }

//        System.out.println(pageForLemmasHtml);
//        System.out.println();
//        System.out.println(lemmasCountByPage);
//        System.out.println();
//        System.out.println(lemmasSet);


//        LemmaModel lemmaModel = new LemmaModel();
//        lemmaModel.setSiteId(siteModel);
//        lemmaModel.setLemma();
//        pageModel.setSiteId(siteModel);
//        pageModel.setPath(url.substring(siteModel.getUrl().length()));
//        pageModel.setCode(statusCode);
//        pageModel.setContent(document.outerHtml());
//        pageRepository.save(pageModel);
    }

    public static String extractText(String html) {
        return Jsoup.parse(html).text();
//        return Jsoup.clean(html, Whitelist.none());
    }

}
