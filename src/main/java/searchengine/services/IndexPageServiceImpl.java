package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;

@Service
public class IndexPageServiceImpl implements IndexPageService{

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

    private Connection.Response response = null;
    private SiteModel siteModel;

//    public boolean isCorrectUrl(String url) throws IOException, IOException {
//        for (Site site : sitesList.getSites()) {
//            if (url.contains(site.getUrl())) {
//                response = Jsoup.connect(url)
//                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
//                                "Chrome/58.0.3029.110 Safari/537.36")
//                        .referrer("http://www.google.com")
//                        .execute();
//                Document doc = response.parse();
//                String htmlContent = doc.getAllElements().toString();
//                siteTable = siteId(site);
//                Page page = saveOrUpdatePage(site, htmlContent, url);
//                saveOrUpdateLemma(doc, page);
//                return true;
//            }
//        }
//        return false;
//    }
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
//    public SiteTable siteId(Site site) {
//        List<SiteTable> sites = siteRepositories.findAll();
//        Optional<SiteTable> matchingSite = sites.stream()
//                .filter(site1 -> site1.getName().equals(site.getName()))
//                .findFirst();
//        return matchingSite.orElse(null);
//    }
}