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
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public boolean startIndexingSite() {
        isThreadsRunning = true;
        sitesList.getSites().forEach((site) -> {
            deleteOldDataByUrlSite(site.getUrl());
            siteModel = siteModelUtil.createNewSiteModel(site);
            log.info("Start indexing site: " + site.getUrl());
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
        log.info("В репозитории находятся SiteModel с указанным URL - " + listModelsToDelete.size());
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
        if (isIndexing()) {
            forkJoinPool.shutdownNow();
            queueLinks.clear();
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
}