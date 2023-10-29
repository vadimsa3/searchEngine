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
    public boolean indexOnePageByUrl(String webPageUrl) {
        String domainWebName = webPageUrl.replaceAll("http(s)?://|www\\.|/.*", "");
        for (Site site : sitesList.getSites()) {
            try {
                if (webPageUrl.contains(site.getUrl())) {
                    // проверка на наличие в репозитории сайтов и в списке конфигурации
                    SiteModel siteModel = getSiteModel(domainWebName, site);
                    System.out.println("****" + siteModel.getName() + " " + siteModel.getStatusTime());

                    log.info("Start indexing single page: " + domainWebName);
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
                    log.info("End indexing single page: " + site.getUrl());

//                    matchingSiteModel(webPageUrl);
//                    SiteModel siteModel = matchingSiteModel(site);
//                    System.out.println("Site model " + siteModel.getName() + "Creating time " + siteModel.getStatusTime());
//                    PageModel pageModel = saveNewOrUpdateOldPage(site, document, siteModel, statusCode, webPageUrl);
//                    lemmaModelUtil.createNewLemmaModel(pageModel, siteModel);
//                    log.info("Page indexing completed: " + site.getUrl());
//                    return true;
                }
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.error("Данная страница находится за пределами сайтов," +
                "указанных в конфигурационном файле");
        return false;
    }


//        for (Site site : sitesList.getSites()) {
//            try {
//                if (webPageUrl.contains(site.getUrl())) {
//                    log.info("Start indexing single page: " + site.getUrl());
//                    Connection.Response response =
//                            Jsoup.connect(webPageUrl)
//                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
//                                            "Gecko/20100101 Firefox/25.0")
//                                    .referrer("http://www.google.com")
//                                    .timeout(3000)
//                                    .ignoreHttpErrors(true)
//                                    .execute();
//                    int statusCode = response.statusCode();
//                    Document document = response.parse();
//
//                    // проверка на наличие в репозитории сайтов и в списке конфигурации
//                    SiteModel siteModel = getSiteModel(domainWebName, site);
//                    System.out.println("****" + siteModel.getName() + " " + siteModel.getStatusTime());
//
////                    matchingSiteModel(webPageUrl);
////                    SiteModel siteModel = matchingSiteModel(site);
////                    System.out.println("Site model " + siteModel.getName() + "Creating time " + siteModel.getStatusTime());
////                    PageModel pageModel = saveNewOrUpdateOldPage(site, document, siteModel, statusCode, webPageUrl);
////                    lemmaModelUtil.createNewLemmaModel(pageModel, siteModel);
////                    log.info("Page indexing completed: " + site.getUrl());
////                    return true;
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return false;
//    }

    /* проверка на наличие в репозитории сайта, вернет null если не найдет или SiteModel
    проверяем по
        если модель сайта есть в репозитории:
     - ничего с моделью сайта не делаем,
     - проверяем на наличие в репозитории страниц наличие страницы по path,
        если есть - обновляем в ней данные (или удаляем в ней данные - должны удалится связанные леммы и индексы +
     парсим страницу и создаем новую с id сайта, модель которого нашли)
        если нет - парсим страницу и создаем новую с id сайта, модель которого нашли.

        если модели сайта нет в репозитории:
     - проверяем на наличие записи с переданным weburl в списке конфигурации,
        если есть - создаем новую модель сайта c weburl, парсим страницу, создаем страницу, леммы, индексы.
        если нет - выводим сообщение о не нахождении в списке.
     */

    //
    public SiteModel getSiteModel(String domainWebName, Site site) {
        SiteModel siteModel = isSiteRepositoryContainsSiteModel(domainWebName);
        if (siteModel == null) {
            System.out.println("!!! Пустой репозиторий или нет модели - Надо что-то делать!!!");
//            // проверяем список конфигурации и добавляем новую модель
//            System.out.println("В списке ? - " + isListSitesContainsWebPageUrl(domainWebName));
            siteModel = siteModelUtil.createNewSiteModel(site);
            log.info("В репозиторий добавлена новая модель сайта - "
                    + siteModel.getUrl() + " "
                    + siteModel.getStatusTime());
        } else {
            // получаем модель из репозитория
            System.out.println("Все ОК " + siteModel.getName() + "  " + siteModel.getStatusTime());
        }
        return siteModel;
    }

    public SiteModel isSiteRepositoryContainsSiteModel(String domainWebName) {
        System.out.println("PAGE DOMAIN NAME IS - " + domainWebName);
        List<SiteModel> siteModelsList = siteRepository.findAll();
        SiteModel newSiteModel = null;
        if (!siteModelsList.isEmpty()) {
            for (SiteModel siteModel : siteModelsList) {
                if (!siteModel.getUrl().contains(domainWebName)) {
                    log.info("В репозитории отсутствует модель сайта с url - " + domainWebName);
                    return null;
                } else {
                    log.info("В репозитории уже есть модель сайта с url - " + domainWebName);
                    newSiteModel = siteModel;
                    System.out.println("ВОТ ОНА " + newSiteModel.getName() + "  " + newSiteModel.getStatusTime());
                }
            }
        } else {
            log.info("В репозитории нет объектов");
            return null;
        }
        return newSiteModel;
    }

//    private Site isListSitesContainsWebPageUrl(String domainWebName) {
//        Site newSite = new Site();
//        for (Site site : sitesList.getSites()) {
//            if (!site.getUrl().contains(domainWebName)) {
//                log.error("Данная страница находится за пределами сайтов," +
//                        "указанных в конфигурационном файле");
//            }
//                log.info("Данная страница находится в пределах сайтов," +
//                        "указанных в конфигурационном файле");
//            newSite = site;
//        }
//        return newSite;
//    }


//    private boolean isListSitesContainsWebPageUrl(String domainWebName, Site site) {
//        if (!site.getUrl().contains(domainWebName)) {
//            log.error("Данная страница находится за пределами сайтов," +
//                    "указанных в конфигурационном файле");
//            return false;
//        }
//        log.info("Данная страница находится в пределах сайтов," +
//                "указанных в конфигурационном файле");
//        return true;
//    }

//    public SiteModel matchingSiteModel(String webPageUrl, Site site) {
//        String domainWebName = webPageUrl.replaceAll("http(s)?://|www\\.|/.*", "");
//        System.out.println("PAGE DOMAIN NAME " + domainWebName);
//        List<SiteModel> siteModelsList = siteRepository.findAll();
//        for (SiteModel siteModel : siteModelsList) {
//            return siteModel.getUrl().contains(domainWebName)
//                    ? siteModel
//                    : isSiteListContainsSiteModel(siteModel, site);
//        }
//    }

//    public SiteModel isSiteListContainsSiteModel(SiteModel siteModel, Site site) {
//        if (!siteModel.getUrl().equals(site.getUrl())) {
//            log.error("Данная страница находится за пределами сайтов," +
//                    "указанных в конфигурационном файле");
//        } return siteModelUtil.createNewSiteModel(site);
//    }


    public void matchingPageModel(String webPageUrl, SiteModel siteModel, Document document, Integer statusCode) {
        String path = webPageUrl.substring(siteModel.getUrl().length());
        PageModel pageModel = pageRepository.findByPath(path);
        if (pageModel != null) {
            pageRepository.delete(pageModel);
            pageModelUtil.createNewPageModel(webPageUrl, document, siteModel, statusCode);
        }
    }

//        public SiteModel matchingSiteModel(Site site) {
////        List<SiteModel> siteModels = siteRepository.findAll();
//
//
//        List<SiteModel> siteModels = siteRepository.findAll();
//        Optional<SiteModel> isMatchingSiteModel = siteModels.stream()
//                .filter(siteModel -> siteModel.getName().equals(site.getName()))
//                .findFirst();
//        return isMatchingSiteModel.orElse(siteModelUtil.createNewSiteModel(site));
//    }


//    public void startIndexSingleSite(Site site) {
//        SiteModel oldSiteModel = siteRepository.findSiteModelByUrl(site.getUrl());
//        if (oldSiteModel != null) {
//            siteIndexingService.deleteOldDataByUrlSite(site.getUrl());
//            siteIndexingService.startParsingSite(site.getUrl());  // reindexing
//        } else {
//            SiteModel siteModel = siteModelUtil.createNewSiteModel(site);
//            log.info("Start indexing single site: " + site.getUrl());
//            siteIndexingService.startParsingSite(site.getUrl());
//            log.info("Count pages from site " + siteModel.getName() + " - " + siteIndexingService.countPagesBySiteId(siteModel));
//            log.info("Site indexing completed: " + site.getUrl());
//        }
//    }

    public PageModel saveNewOrUpdateOldPage(Site site, Document document, SiteModel siteModel,
                                            Integer statusCode, String webPageUrl) {
        String path = webPageUrl.replaceAll(site.getUrl(), "");
        // ПЕРЕЗАПИШЕМ ДАННЫЕ В СУЩЕСТВУЮЩЕЙ ЗАПИСИ ЕСЛИ ОНА ЕСТЬ ИЛИ СОЗДАЕМ НОВУЮ СТРАНИЦ
        Optional<PageModel> existingPage = pageRepository.findPageByPath(path);
        if (existingPage.isPresent()) {
            PageModel existingPageModel = existingPage.get();
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