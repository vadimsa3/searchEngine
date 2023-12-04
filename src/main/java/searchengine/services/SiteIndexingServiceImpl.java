package searchengine.services;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.*;

@Service
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);

    private ForkJoinPool forkJoinPool;

    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteModelUtil siteModelUtil;
    @Autowired
    private PageModelUtil pageModelUtil;
    @Autowired
    private LemmaModelUtil lemmaModelUtil;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;

    @Getter
    private static String domainName;

    private static Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
    private static final Queue<String> queueLinks = new ConcurrentLinkedQueue<>();
    private static final HashMap<Integer, String> lastError = new HashMap<>();
    private SiteModel siteModel;
    private Boolean isInterrupted = null;
    private Boolean isThreadsRunning = null;
    private final Map<String, Boolean> indexingStatus;

    public SiteIndexingServiceImpl() {
        forkJoinPool = new ForkJoinPool();
        indexingStatus = new HashMap<>();
    }

    public boolean startIndexingSite() {
        isThreadsRunning = true;
        sitesList.getSites().forEach((site) -> {
            indexingStatus.put(site.getUrl(), false);
            deleteOldDataByUrlSite(site.getUrl());
            siteModel = siteModelUtil.createNewSiteModel(site, StatusSiteIndex.INDEXING);
            log.info("Запущена индексация сайта: " + site.getUrl());
            isThreadsRunning = startParsingSite(site.getUrl());

            if (isThreadsRunning = false) {
                siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
                        LocalDateTime.now(), "Ошибка индексации сайта");
                indexingStatus.put(site.getUrl(), false);
                log.error("Ошибка индексации сайта");
            } else {
                indexingStatus.put(site.getUrl(), true);
                log.info("Процесс индексации сайта завершен успешно: " + site.getUrl());
                log.info("Страниц на сайте - " + siteModel.getName() + " - " + countPagesBySiteId(siteModel));
            }
        });
        return isThreadsRunning;
    }

    public boolean startParsingSite(String url) {
        String[] tmpArray = url.split("/");
        domainName = tmpArray[2];
        queueLinks.add(url);
        List<ParserSiteUtil> taskListLinkParsers = new ArrayList<>();
        for (int threads = 0; threads < Runtime.getRuntime().availableProcessors(); ++threads) {
            ParserSiteUtil parser = new ParserSiteUtil(queueLinks, visitedLinks, siteRepository,
                    pageRepository, siteModel, lastError, siteModelUtil, pageModelUtil, lemmaModelUtil, lemmaFinderUtil);
            taskListLinkParsers.add(parser);
        }
        if (!forkJoinPool.isShutdown()) {
            taskListLinkParsers.forEach(forkJoinPool::invoke);
            taskListLinkParsers.forEach(parserSiteUtil -> {
                if (parserSiteUtil.getStatus().equals("working")) {
                    try {
                        Thread.sleep(1000L);
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
        log.info("В репозитории уже находятся SiteModel с указанным URL - " + listModelsToDelete.size());
        if (!listModelsToDelete.isEmpty()) {
            for (SiteModel siteModelToDelete : listModelsToDelete) {
                siteRepository.delete(siteModelToDelete);
                log.info("Успешно удалены устаревшие данные по сайту - " + siteModelToDelete.getUrl());
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

    @Override
    public boolean stopIndexingSite() {
        return isShutdown();


//        log.info("Индексация запущена? " + isIndexing());
//        try {
//            forkJoinPool.shutdownNow();
//            boolean close = forkJoinPool.awaitTermination(1, TimeUnit.SECONDS);
//            System.out.println("ОСТАНОВЛЕНО? " + close);
//        } catch (Exception exception) {
//            log.error(exception.getMessage(), exception);
//            lastError.put(siteModel.getId(), exception.getMessage());
//        }
//        return true;
    }

    public boolean isShutdown(){
        return true;
//        return forkJoinPool.isShutdown();
    }


//                System.out.println("IS STOP INDEXING ?" + !forkJoinPool.isShutdown()); // !!!!! DELETE !!!!
//                restartForkJoinPool();
//                log.info("Индексация остановлена пользователем!");
//                System.out.println("Очередь " + queueLinks);
//                siteRepository.findAll().forEach(siteModel -> {
//                    if (siteModel.getStatusSiteIndex() != StatusSiteIndex.INDEXED) {
//                        siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
//                                LocalDateTime.now(), "Индексация остановлена пользователем!");
//                    }
//                });
//            } catch (Exception exception) {
//                log.error(exception.getMessage(), exception);
//                lastError.put(siteModel.getId(), exception.getMessage());
//            }
//            return true;
//    }

        // !!! поработать над методом
//        if (!forkJoinPool.isShutdown()) {
//            indexingStatus.keySet().forEach(urlSite -> {
//                Boolean isIndOk = indexingStatus.get(urlSite);
//                if (!isIndOk) {
//                    siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
//                            LocalDateTime.now(), "Indexing stopped by user!");
//                }
//            });
//            forkJoinPool.shutdownNow();
//            queueLinks.clear();
//            restartForkJoinPool();
//            return true;
//        } else {
//            log.info("Indexing not stopped! Start indexing process before stopped!");
//        }
//        return false;
//    }

//        if (isIndexing()) {
//            forkJoinPool.shutdownNow();
//            System.out.println("IS STOP INDEXING ?" + !forkJoinPool.isShutdown()); // !!!!! DELETE !!!!
//            queueLinks.clear();
//            log.info("Индексация остановлена пользователем!");
//            System.out.println("Очередь " + queueLinks);
//            siteRepository.findAll().forEach(siteModel -> {
//                if (siteModel.getStatusSiteIndex() != StatusSiteIndex.INDEXED) {
//                    siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
//                            LocalDateTime.now(), "Индексация остановлена пользователем!");
//                }
//            });
//            restartForkJoinPool();
//            return true;
//        } else {
//            log.info("Индексация не остановлена! Начните индексацию перед остановкой!");
//        }
//        return false;
//    }

    private void restartForkJoinPool() {
        forkJoinPool = new ForkJoinPool();
    }
}