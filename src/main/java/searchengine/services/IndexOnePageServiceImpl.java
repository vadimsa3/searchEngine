package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.LemmaModelUtil;
import searchengine.utilities.PageModelUtil;
import searchengine.utilities.SiteModelUtil;

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
    @Autowired
    private LemmaModelUtil lemmaModelUtil;
    @Autowired
    private SiteModelUtil siteModelUtil;
    @Autowired
    private SiteIndexingServiceImpl siteIndexingService;
    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);
    private Boolean isThreadsRunning = null;

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

    // проверка отношения страницы к сайту из списка и репозитория
    @Override
    public boolean isCorrectPageUrl(String webPageUrl) {
//        sitesList.getSites().forEach(site -> {
        for (Site site : sitesList.getSites()) {
            try {
                if (webPageUrl.contains(site.getUrl())) {
                    log.info("Start indexing single page: " + site.getUrl());
                    Connection.Response response =
                            Jsoup.connect(webPageUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                                            "Gecko/20100101 Firefox/25.0")
                                    .referrer("http://www.google.com")
                                    .timeout(3000)
                                    .ignoreHttpErrors(true)
                                    .execute();
                    int statusCode = response.statusCode();
                    Document document = response.parse();
                    // проверка на наличие в репозитории сайтов
                    SiteModel siteModel = matchingSiteModel(site);
                    PageModel pageModel = saveNewOrUpdateOldPage(site, document, siteModel, statusCode, webPageUrl);
                    lemmaModelUtil.createNewLemmaModel(pageModel, siteModel);
                    log.info("Page indexing completed: " + site.getUrl());
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    // проверка на наличие в репозитории сайта, вернет null если не найдет или SiteModel
    public SiteModel matchingSiteModel(Site site) {
        List<SiteModel> siteModels = siteRepository.findAll();
        Optional<SiteModel> isMatchingSiteModel = siteModels.stream()
                .filter(siteModel -> siteModel.getName().equals(site.getName()))
                .findFirst();
        return isMatchingSiteModel.orElse(siteModelUtil.createNewSiteModel(site));

//
//        if (siteModel != null) {
//            siteIndexingService.deleteOldDataByUrlSite(site.getUrl());
//
//            siteIndexingService.startParsingSite(site.getUrl());  // reindexing
//        } else {
//            SiteModel siteModel = siteModelUtil.createNewSiteModel(site);
//            log.info("Start indexing single site: " + site.getUrl());
//            siteIndexingService.startParsingSite(site.getUrl());
//            log.info("Count pages from site " + siteModel.getName() + " - "
//                    + siteIndexingService.countPagesBySiteId(siteModel));
//            log.info("Site indexing completed: " + site.getUrl());
//        }
    }
    public void startIndexSingleSite(Site site) {
        SiteModel oldSiteModel = siteRepository.findSiteModelByUrl(site.getUrl());
        if (oldSiteModel != null) {
            siteIndexingService.deleteOldDataByUrlSite(site.getUrl());
            siteIndexingService.startParsingSite(site.getUrl());  // reindexing
        } else {
            SiteModel siteModel = siteModelUtil.createNewSiteModel(site);
            log.info("Start indexing single site: " + site.getUrl());
            siteIndexingService.startParsingSite(site.getUrl());
            log.info("Count pages from site " + siteModel.getName() + " - " + siteIndexingService.countPagesBySiteId(siteModel));
            log.info("Site indexing completed: " + site.getUrl());
        }
    }

    public PageModel saveNewOrUpdateOldPage(Site site, Document document, SiteModel siteModel,
                                            Integer statusCode, String webPageUrl) {
        String path = webPageUrl.replaceAll(site.getUrl(), "");
//        String path = webPageUrl.substring(siteModel.getUrl().length());
        Optional<PageModel> existingPageOpt = pageRepository.findPageByPath(path);
        if (existingPageOpt.isPresent()) {
            PageModel existingPageModel = existingPageOpt.get();
            existingPageModel.setSiteId(siteModel);
            existingPageModel.setCode(statusCode);
            existingPageModel.setPath(path);
            existingPageModel.setContent(document.outerHtml());
            pageRepository.save(existingPageModel);
            return existingPageModel;
        } else {
            return pageModelUtil.createNewPageModel(webPageUrl, document, siteModel, statusCode);
//            PageModel pageModel = new PageModel();
//            newPage.setSiteId(siteTable);
//            newPage.setCode(response.statusCode());
//            newPage.setPath(path);
//            newPage.setContent(content);
//            pageRepositories.save(newPage);
//            return newPage;
        }
    }
}

//    public void saveNewOrUpdateOldLemma(PageModel pageModel, SiteModel siteModel) throws IOException {
//        LemmatizationUtils lemmas = new LemmatizationUtils();
//        String htmlText = document.text();
//        Map<String, Integer> lemmasMap = lemmas.getLemmaMap(htmlText);
//
//        for (String word : lemmasMap.keySet()) {
//            int countLemma = lemmasMap.get(word);
//            Optional<Lemma> existingLemmaOpt = lemmaRepositories.findByLemma(word);
//            if (existingLemmaOpt.isPresent()) {
//                Lemma existingLemma = existingLemmaOpt.get();
//                int count = existingLemma.getFrequency() + countLemma;
//                existingLemma.setFrequency(count);
//                lemmaRepositories.save(existingLemma);
//                saveSearchIndex(page, existingLemma, count);
//            } else {
//                Lemma newLemma = new Lemma();
//                newLemma.setSiteId(siteTable);
//                newLemma.setFrequency(countLemma);
//                newLemma.setLemma(word);
//                lemmaRepositories.save(newLemma);
//                saveSearchIndex(page, newLemma, countLemma);
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
