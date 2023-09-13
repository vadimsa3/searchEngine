package searchengine.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Service
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);

    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    private static String domainName;
    private static Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
    private static Queue<String> queueLinks = new ConcurrentLinkedQueue();
    private static final HashMap<Integer, String> lastError = new HashMap();
    private SiteModel siteModel;
    private String status = null;
    private Boolean interrupted = false;
    private Boolean threadsRunning = null;

    public void startIndexingSite() {

        sitesList.getSites().forEach((site) -> {
            deleteOldDataByUrlSite(site.getUrl());
            siteModel = createSiteModel(site);
            log.info("Start indexing site: " + site.getUrl());
            startParsingSite(site.getUrl());
            log.info("Count pages from site " + siteModel.getName() + " - " + countPagesFromSite(siteModel.getId()));
            log.info("Site indexing completed: " + site.getUrl());
        });
    }

    public void startParsingSite(String url) {
        String[] tmpArray = url.split("/");
        domainName = tmpArray[2];
        queueLinks.add(url);
        List<ParserSite> taskListLinkParsers = new ArrayList<>();

        for (int threads = 0; threads < Runtime.getRuntime().availableProcessors(); ++threads) {
            ParserSite parser = new ParserSite(queueLinks, visitedLinks, siteRepository,
                    pageRepository, siteModel, lastError);
            taskListLinkParsers.add(parser);
        }

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        taskListLinkParsers.forEach(forkJoinPool::invoke);
        // !!! execute организует (асинхронное) выполнение данной задачи (выводит ошибку 500 во фронте) !!!
        taskListLinkParsers.forEach(parserSite -> {
            if (parserSite.getStatus().equals("working")) {
                try {
                    Thread.sleep(1000L); // переводит текущий поток в режим ожидания, замедление
                } catch (InterruptedException interruptedException) {
                    throw new RuntimeException(interruptedException);
                }
            }
        });
    }

    private SiteModel createSiteModel(Site site) {
        SiteModel siteModel = new SiteModel();
        siteModel.setStatusSiteIndex(StatusSiteIndex.INDEXING);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setLastError(lastError.get(siteModel.getId()));
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteRepository.save(siteModel);
        return siteModel;
    }

    public static String getDomainName() {
        return domainName;
    }

    public long countPagesFromSite(Integer siteId) {
        return pageRepository.count();
    }

    public void deleteOldDataByUrlSite(String urlSite) {
        SiteModel siteModelToDelete = siteRepository.findSiteModelByUrl(urlSite);
        if (siteModelToDelete != null) {
            pageRepository.deleteAllDataById(siteModelToDelete.getId());
            siteRepository.delete(siteModelToDelete);
        }
    }

//    public SiteModel getOldSiteModelByUrlSite(String urlSite) {
//        siteRepository.findAll().forEach(siteModel -> {
//            if (siteModel.getUrl().startsWith(urlSite)) {
//            }
//        });
//        return siteModel;
//    }
//
//
//    public List<Integer> getAllIdPagesBySiteId(String urlSite) {
//        List<Integer> idPages = new ArrayList<>();
//        pageRepository.findAllIdPagesBySiteId(getOldSiteModelByUrlSite(urlSite)).forEach(page -> {
//            idPages.add(page.getId());
//        });
//        return idPages;
//    }
//
//    public void deleteAllPagesBySiteId(List<Integer> idPages) {
//        pageRepository.deleteAllById(idPages);
//    }

    @Override
    public boolean stopIndexingSite() {
        if (!threadsRunning) {
            interrupted = true;
            log.error("Indexing error! Indexing has been stopped by the user.");
            String error = "Indexing error! Indexing has been stopped by the user.";
            lastError.put(siteModel.getId(), error);
        }
        return false;

//        if(!isInterrupted) {
//
//            for (String singleLink : getUrl(rootUrl)) {
//                if (!parsedLinks.contains(singleLink)) {
//                    SiteParser task = new SiteParser(sin.......
//                }
//                if (isInterrupted) {
//                    taskList.clear();
//                }
//                ну и в stopIndexing isInterrupted = true
//            }
//
//            private boolean isInterrupted () {
//                return threadsRunning ? interrupted : true;
    }
}