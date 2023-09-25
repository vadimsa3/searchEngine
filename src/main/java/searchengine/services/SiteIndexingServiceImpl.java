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
    private Boolean isInterrupted = null;
    private Boolean isThreadsRunning = null;

    public boolean startIndexingSite() {
//        isThreadsRunning = true;
//        log.info("BEGIN" + isThreadsRunning);
//        interrupted = false;
                sitesList.getSites().forEach((site) -> {
                    deleteOldDataByUrlSite(site.getUrl());
                    siteModel = createSiteModel(site);
                    log.info("Start indexing site: " + site.getUrl());
//                    startParsingSite(site.getUrl());
                    isThreadsRunning = startParsingSite(site.getUrl());
                    log.info("Count pages from site " + siteModel.getName() + " - " + countPagesBySiteId(siteModel));
                    log.info("Site indexing completed: " + site.getUrl());
                });
        return isThreadsRunning;
    }

    public boolean startParsingSite(String url) {
        String[] tmpArray = url.split("/");
        domainName = tmpArray[2];
        queueLinks.add(url);
        List<ParserSiteService> taskListLinkParsers = new ArrayList<>();
        for (int threads = 0; threads < Runtime.getRuntime().availableProcessors(); ++threads) {
            ParserSiteService parser = new ParserSiteService(queueLinks, visitedLinks, siteRepository,
                    pageRepository, siteModel, lastError);
            taskListLinkParsers.add(parser);
        }
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        if (!forkJoinPool.isShutdown()) { // Returns true if this executor terminated
            taskListLinkParsers.forEach(forkJoinPool::invoke);
            // !!! execute организует (асинхронное) выполнение данной задачи (выводит ошибку 500 во фронте) !!!
            taskListLinkParsers.forEach(parserSiteService -> {
                if (parserSiteService.getStatus().equals("working")) {
                    try {
                        Thread.sleep(1000L); // переводит текущий поток в режим ожидания, замедление
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }
                }
            });
            return false;
        } else {
            return true;
        }
    }

    public Boolean isIndexing(){
        return siteRepository.findAllSitesBySiteStatus(StatusSiteIndex.INDEXING).size() != 0;
    }

//    public boolean isIndexing(){
//        return siteRepository.countByStatus(StatusSiteIndex.INDEXING) != 0;
//    }


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

    public Integer countPagesBySiteId(SiteModel siteModel) {
        return (pageRepository.findAllPagesBySiteId(siteModel).size());
    }

//    public boolean isIndexing(){
//        return siteRepository.countByStatus(StatusSiteIndex.INDEXING) != 0;
//    }

    public void deleteOldDataByUrlSite(String urlSite) {
        SiteModel siteModelToDelete = siteRepository.findSiteModelByUrl(urlSite);
        if (siteModelToDelete != null) {
            siteRepository.delete(siteModelToDelete);
            pageRepository.deleteAllDataById(siteModelToDelete.getId());
        }
    }

    @Override
    public boolean stopIndexingSite() { // переделать
        if (!isThreadsRunning) {
//          isInterrupted = true;
            log.error("Indexing error! Indexing has been stopped by user.");
            String error = "Indexing error! Indexing has been stopped by user.";
            lastError.put(siteModel.getId(), error);
        }
        return false;
    }

//    public boolean stopIndexing() {
//        System.out.println("Потоков работает: " + threads.size());
//
//        AtomicBoolean isIndexing = new AtomicBoolean(false);
//
//        siteRepository.findAll().forEach(site -> {
//            if (site.getStatus().equals(Status.INDEXING)) {
//                isIndexing.set(true);
//            }
//        });
//
//        if (!isIndexing.get()) {
//            return true;
//        }
//
//        forkJoinPools.forEach(ForkJoinPool::shutdownNow);
//        threads.forEach(Thread::interrupt);
//
//        siteRepository.findAll().forEach(site -> {
//            site.setLastError("Остановка индексации");
//            site.setStatus(Status.FAILED);
//            siteRepository.save(site);
//        });
//
//        threads.clear();
//        forkJoinPools.clear();
//
//        return false;
//    }




    @Override
    public boolean startIndexSingleSite(Site site) {
        SiteModel oldSiteModel = siteRepository.findSiteModelByUrl(site.getUrl());
        if (oldSiteModel == null) {
            siteModel = createSiteModel(site);
            log.info("Start indexing single site: " + site.getUrl());
            startParsingSite(site.getUrl());
            log.info("Count pages from site " + siteModel.getName() + " - " + countPagesBySiteId(siteModel));
            log.info("Site indexing completed: " + site.getUrl());
        } else {
            deleteOldDataByUrlSite(site.getUrl());
            isThreadsRunning = startParsingSite(site.getUrl());  // reindexing
        }
        return isThreadsRunning;
    }
}

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
