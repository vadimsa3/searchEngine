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
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageModel;
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

        pageRepository.deleteAll();
        siteRepository.deleteAll();

        sitesList.getSites().forEach((site) -> {

//            deleteAllPagesBySite(site.getUrl());

            siteModel = createSiteModel(site);
            log.info("Start indexing site: " + site.getUrl());
            startParsingSite(site.getUrl());
            log.info("Count pages from site " + siteModel.getName() + " - " + countPagesFromSite(siteModel.getId()));
            findPagesIdBySiteIdInDB(siteModel.getId());
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

        ForkJoinPool var10001 = new ForkJoinPool();
        taskListLinkParsers.forEach(var10001::invoke);
        taskListLinkParsers.forEach((parserSite) -> {
            if (parserSite.getStatus().equals("working")) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var2) {
                    throw new RuntimeException(var2);
                }
            }

        });

        deleteAllPagesBySiteId(getAllIdPagesBySiteId(siteModel));

    }

    private SiteModel createSiteModel(Site site) {
        SiteModel siteModel = new SiteModel();
        siteModel.setStatusSiteIndex(StatusSiteIndex.INDEXING);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setLastError((String) lastError.get(siteModel.getId()));
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteRepository.save(siteModel);
        return siteModel;
    }

    public static String getDomainName() {
        return domainName;
    }

    private long countPagesFromSite(Integer siteId) {
        return pageRepository.count();
    }

    public List<Integer> findSitesIdByUrlInDB(String url) {
        List<Integer> listSiteModelId = new ArrayList<>();
        siteRepository.findAll().forEach(site -> {
            if (site.getUrl().startsWith(url)) {
                listSiteModelId.add(site.getId());
                System.out.println("Sites to data delete " + listSiteModelId.size());
            }
        });
        return listSiteModelId;
    }

    public void findPagesIdBySiteIdInDB(Integer siteId) {
        List<Integer> listPageModelId = new ArrayList<>();
        pageRepository.findAll().forEach(page -> {
            if (page.getSiteId().getId() == siteId) {
                listPageModelId.add(page.getId());
            }
        });
        System.out.println("Pages " + listPageModelId.size());
    }

    public List<Integer> getAllIdPagesBySiteId(SiteModel siteModel) {
        List<Integer> idPages = new ArrayList<>();
        pageRepository.findAllIdPagesBySiteId(siteModel).forEach(page -> {
            idPages.add(page.getId());
        });
        return idPages;
    }

    public void deleteAllPagesBySiteId(List<Integer> idPages) {
        pageRepository.deleteAllById(idPages);
    }

    public void deleteAllPagesBySite(String url) {
        siteRepository.findAll().forEach(siteModel -> {
            if (siteModel.getUrl().startsWith(url)) {
                pageRepository.findAll().forEach(pageModel -> {
                    if (pageModel.getSiteId().getId() == siteModel.getId()) {
                        pageRepository.deleteById(pageModel.getId());
                    }
                });
            }
        });
    }

    public boolean stopIndexingSite() {
        if (!threadsRunning) {
            interrupted = true;
            String error = "Ошибка индексации! Индексация остановлена пользователем.";
            lastError.put(siteModel.getId(), error);
        }
        return false;
    }

    private boolean isInterrupted() {
        return threadsRunning ? interrupted : true;
    }

    public SiteIndexingServiceImpl() {
    }
}