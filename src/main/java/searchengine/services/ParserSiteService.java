package searchengine.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

public class ParserSiteService extends RecursiveAction {

    private static final Logger log = LoggerFactory.getLogger(ParserSiteService.class);

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SiteModel siteModel;
    @Autowired
    private SiteModelService siteModelService;
    @Autowired
    private PageModelService pageModelService;

    private Queue<String> queueLinks;
    private Set<String> visitedLinks;
    private Map<Integer, String> lastError;
    private String status = null;
    private Integer code = null;

    public ParserSiteService(Queue<String> queueLinks, Set<String> visitedLinks, SiteRepository siteRepository,
                             PageRepository pageRepository, SiteModel siteModel, Map<Integer, String> lastError,
                             SiteModelService siteModelService, PageModelService pageModelService) {
        this.queueLinks = queueLinks;
        this.visitedLinks = visitedLinks;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.siteModel = siteModel;
        this.lastError = lastError;
        this.siteModelService = siteModelService;
        this.pageModelService = pageModelService;
    }

    public String getStatus() {
        return status;
    }

    protected void compute() {
        while (true) {
            String link = queueLinks.poll(); // забираем ссылку из очереди
            if (link == null) {
                status = "waiting";
                siteModelService.updateSiteModel(siteModel, StatusSiteIndex.INDEXED, LocalDateTime.now(),
                        lastError.get(siteModel.getId()));
                return;
            }
            if (!visitedLinks.contains(link)) {
                status = "working";
                visitedLinks.add(link);
                log.info("Site link - " + link);
                try {
                    Connection.Response response = Jsoup.connect(link).ignoreHttpErrors(true).execute();
                    int statusCode = response.statusCode();
                    Document document = response.parse();
//                    Document document = Jsoup.connect(link).ignoreHttpErrors(true).get();
                    pageModelService.createPageModel(link, document, siteModel, statusCode);
                    Elements urls = document.getElementsByTag("a");
                    urls.forEach((innerLink) -> {
                        synchronized (queueLinks) {
                            String linkString = innerLink.absUrl("href");
                            if (linkString.contains(SiteIndexingServiceImpl.getDomainName())
                                    & !visitedLinks.contains(linkString)
                                    && linkString.startsWith(link)
                                    && !isFile(linkString)) {
                                queueLinks.add(linkString);
                                ParserSiteService parserSiteService = new ParserSiteService(queueLinks, visitedLinks,
                                        siteRepository, pageRepository, siteModel, lastError, siteModelService,
                                        pageModelService);
                                parserSiteService.fork();
                                siteModelService.updateSiteModel(siteModel, StatusSiteIndex.INDEXING,
                                        LocalDateTime.now(), lastError.get(siteModel.getId()));
                            }
                        }
                    });
                } catch (Exception exception) {
                    log.error(exception.getMessage(), exception);
                    lastError.put(siteModel.getId(), exception.getMessage());
                }
            }
        }
    }

    private static boolean isFile(String link) {
        String reg = "([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf))$)";
        return link.matches(reg);
    }
}