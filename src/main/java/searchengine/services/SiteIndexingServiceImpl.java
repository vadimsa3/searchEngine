package searchengine.services;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.*;

@Service
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);

    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteModelUtil siteModelUtil;
    @Autowired
    private PageModelUtil pageModelUtil;
    @Autowired
    private LemmaModelUtil lemmaModelUtil;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;
    @Autowired
    private IndexRepository indexRepository;

    @Getter
    private static String domainName;

    private static Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
    private static Queue<String> queueLinks = new ConcurrentLinkedQueue();
    private static final HashMap<Integer, String> lastError = new HashMap();
    private SiteModel siteModel;
    private Boolean isInterrupted = null;
    private Boolean isThreadsRunning = null;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public boolean startIndexingSite() {
        isThreadsRunning = true;
//        log.info("BEGIN" + isThreadsRunning);
//        interrupted = false;
        sitesList.getSites().forEach((site) -> {
            isIndexing(); // потом удалить
            System.out.println(isIndexing()); // потом удалить
            deleteOldDataByUrlSite(site.getUrl());
            siteModel = siteModelUtil.createNewSiteModel(site);
            log.info("Start indexing site: " + site.getUrl());
            isIndexing(); // потом удалить
            System.out.println(isIndexing()); // потом удалить
            isThreadsRunning = startParsingSite(site.getUrl());
            log.info("Count pages from site " + siteModel.getName() + " - " + countPagesBySiteId(siteModel));
            log.info("Site indexing completed: " + site.getUrl());
            isIndexing(); // потом удалить
            System.out.println(isIndexing()); // потом удалить
        });
        return isThreadsRunning;
    }

    public boolean startParsingSite(String url) {
        String[] tmpArray = url.split("/");
        domainName = tmpArray[2];
//        или так проверить
//        String domainWebName = url.replaceAll("http(s)?://|www\\.|/.*", "");
        queueLinks.add(url);
        List<ParserSiteUtil> taskListLinkParsers = new ArrayList<>();
        for (int threads = 0; threads < Runtime.getRuntime().availableProcessors(); ++threads) {
            ParserSiteUtil parser = new ParserSiteUtil(queueLinks, visitedLinks, siteRepository,
                    pageRepository, siteModel, lastError, siteModelUtil, pageModelUtil, lemmaModelUtil, lemmaFinderUtil);
            taskListLinkParsers.add(parser);
        }
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
        if (!forkJoinPool.isShutdown()) { // Returns true if this executor terminated
            taskListLinkParsers.forEach(forkJoinPool::invoke);
            // !!! execute организует (асинхронное) выполнение данной задачи (выводит ошибку 500 во фронте) !!!
            taskListLinkParsers.forEach(parserSiteUtil -> {
                if (parserSiteUtil.getStatus().equals("working")) {
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

    public Integer countPagesBySiteId(SiteModel siteModel) {
        return (pageRepository.findAllPagesBySiteId(siteModel).size());
    }

    public void deleteOldDataByUrlSite(String urlSite) {
        List<SiteModel> listModelsToDelete = siteRepository.findSiteModelsByUrl(urlSite);
        System.out.println("В репозитории находятся SiteModel с указанным URL - " + listModelsToDelete.size());
        if (!listModelsToDelete.isEmpty()) {
            for (SiteModel siteModelToDelete : listModelsToDelete) {
                System.out.println("Кандидат на удаление из siteRepository - " + siteModelToDelete.getUrl());
                siteRepository.delete(siteModelToDelete);
            }
        }
    }

    @Override
    public boolean isIndexing() {
        List<SiteModel> list = new ArrayList<>();
        siteRepository.findAll().forEach(siteModel -> {
            if (siteModel.getStatusSiteIndex() == StatusSiteIndex.INDEXING) {
                list.add(siteModel);
            }
        });
        return !list.isEmpty();
    }

    // !!! НАДО ДОРАБОТАТЬ НЕ ОСТАНАВЛИВАЕТ
    // НАДО ЧИСТИТЬ СПИСОК ЗАДАЧ, ТОГДА ДОЛЖЕН САМ ОСТАНОВИТСЯ
    @Override
    public boolean stopIndexingSite() {
        if (isIndexing()) {
            forkJoinPool.shutdownNow();
            log.info("Indexing stopped by user!");
            siteRepository.findAll().forEach(siteModel -> {
                if (siteModel.getStatusSiteIndex() != StatusSiteIndex.INDEXED) {
                    siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
                            LocalDateTime.now(), "Indexing stopped by user!");
                }
            });
            return true;
        } else {
            log.info("Indexing not stopped! Start indexing process before stopped!");
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


    // НА УДАЛЕНИЕ ВЕРОЯТНО
    public boolean startIndexSingleSite(Site site) {
        SiteModel oldSiteModel = siteRepository.findSiteModelByUrl(site.getUrl());
        if (oldSiteModel == null) {
            siteModel = siteModelUtil.createNewSiteModel(site);
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
