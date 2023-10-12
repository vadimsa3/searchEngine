package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.PageModelUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class IndexOnePageServiceImpl implements IndexOnePageService {

    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private PageModelUtil pageModelUtil;
    private SiteModel siteModel;
    private PageModel pageModel;

    /*
    Проверьте работу индексации на отдельной странице, указав путь к ней в
    веб-интерфейсе вашего приложения и запустив её индексацию.

    Не забудьте, что при добавлении страницы в базу данных она должна
    привязываться к записи в таблице site, которая либо уже должна там
    находиться, либо должна быть создана на основе одного из пунктов
    списка сайтов в конфигурации вашего приложения.

    ● В случае попытки индексации страницы с какого-то другого сайта
    команда API должна выдавать ошибку в соответствии с технической
    спецификацией. Убедитесь в этом в веб-интерфейсе вашего
    приложения.

    ● В случае, если переданная страница уже была проиндексирована, перед
    её индексацией необходимо удалить всю информацию о ней из таблиц
    page, lemma и index.
     */

    public boolean isCorrectPageUrl(String url) {
        sitesList.getSites().forEach(site -> {
            if (url.contains(site.getUrl())) {
                try {
                    Connection.Response response = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                                    "Gecko/20100101 Firefox/25.0")
                            .referrer("http://www.google.com")
                            .timeout(3000)
                            .ignoreHttpErrors(true)
                            .execute();
                    int statusCode = response.statusCode(); // может и не пригодится
                    Document document = response.parse();

                    siteModel = siteId(site);
                    pageModel = pageModelUtil.createPageModel(url, document, siteModel, statusCode);
                    saveOrUpdateLemma(document, page);

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                return true;
            }
        });
        return false;
    }

    //
//    public Page saveOrUpdatePage(Site site, String content, String url){
//        String path = url.replaceAll(site.getUrl(),"");
//        Optional<Page> existingPageOpt  = pageRepositories.findByPath(path);
//        if(existingPageOpt.isPresent()){
//            Page existingPage = existingPageOpt.get();
//            existingPage.setSiteId(siteTable);
//            existingPage.setCode(response.statusCode());
//            existingPage.setPath(path);
//            existingPage.setContent(content);
//            pageRepositories.save(existingPage);
//            return existingPage;
//        }else {
//            Page newPage = new Page();
//            newPage.setSiteId(siteTable);
//            newPage.setCode(response.statusCode());
//            newPage.setPath(path);
//            newPage.setContent(content);
//            pageRepositories.save(newPage);
//            return newPage;
//        }
//    }
//
//    public void saveOrUpdateLemma(Document doc,Page page) throws IOException {
//        LemmatizationUtils lemmas = new LemmatizationUtils();
//        String htmlText = doc.text();
//        Map<String,Integer> lemmasMap = lemmas.getLemmaMap(htmlText);
//        for(String word : lemmasMap.keySet() ){
//            int countLemma = lemmasMap.get(word);
//            Optional<Lemma> existingLemmaOpt = lemmaRepositories.findByLemma(word);
//            if(existingLemmaOpt.isPresent()){
//                Lemma existingLemma = existingLemmaOpt.get();
//                int count = existingLemma.getFrequency() + countLemma;
//                existingLemma.setFrequency(count);
//                lemmaRepositories.save(existingLemma);
//                saveSearchIndex(page,existingLemma,count);
//            }else {
//                Lemma newLemma = new Lemma();
//                newLemma.setSiteId(siteTable);
//                newLemma.setFrequency(countLemma);
//                newLemma.setLemma(word);
//                lemmaRepositories.save(newLemma);
//                saveSearchIndex(page,newLemma,countLemma);
//            }
//        }
//    }
//
//    public void saveSearchIndex(Page page,Lemma lemma,int count){
//        SearchIndex searchIndex = new SearchIndex();
//        searchIndex.setPageId(page);
//        searchIndex.setLemmaId(lemma);
//        searchIndex.setRank(count);
//        searchIndexRepositories.save(searchIndex);
//    }
//
    public SiteModel siteId(Site site) {
        List<SiteModel> sites = (List<SiteModel>) siteRepository.findAll();
        Optional<SiteModel> matchingSite = sites.stream()
                .filter(site1 -> site1.getName().equals(site.getName()))
                .findFirst();
        return matchingSite.orElse(null);
    }
}