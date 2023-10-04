package searchengine.utilities;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteIndexingServiceImpl;

public class ParserSiteUtil extends RecursiveAction {

    private static final Logger log = LoggerFactory.getLogger(ParserSiteUtil.class);

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SiteModel siteModel;
    @Autowired
    private SiteModelUtil siteModelUtil;
    @Autowired
    private PageModelUtil pageModelUtil;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;
    @Autowired
    private LemmaModelUtil lemmaModelUtil;

    private Queue<String> queueLinks;
    private Set<String> visitedLinks;
    private Map<Integer, String> lastError;
    private String status = null;

    public ParserSiteUtil(Queue<String> queueLinks, Set<String> visitedLinks, SiteRepository siteRepository,
                          PageRepository pageRepository, SiteModel siteModel, Map<Integer, String> lastError,
                          SiteModelUtil siteModelUtil, PageModelUtil pageModelUtil, LemmaModelUtil lemmaModelUtil,
                          LemmaFinderUtil lemmaFinderUtil) {
        this.queueLinks = queueLinks;
        this.visitedLinks = visitedLinks;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.siteModel = siteModel;
        this.lastError = lastError;
        this.siteModelUtil = siteModelUtil;
        this.pageModelUtil = pageModelUtil;
        this.lemmaModelUtil = lemmaModelUtil;
        this.lemmaFinderUtil = lemmaFinderUtil;
    }

    public String getStatus() {
        return status;
    }

    protected void compute() {
        while (true) {
            String link = queueLinks.poll(); // забираем ссылку из очереди
            if (link == null) {
                status = "waiting";
                siteModelUtil.updateSiteModel(siteModel, StatusSiteIndex.INDEXED, LocalDateTime.now(),
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
                    PageModel pageModel = pageModelUtil.createPageModel(link, document, siteModel, statusCode);
                    lemmaModelUtil.createLemmaModel(pageModel);

// СЮДА ВПИСАТЬ ПОЛУЧЕНИЕ ЛЕММ

                    Elements urls = document.getElementsByTag("a");
                    urls.forEach((innerLink) -> {
                        synchronized (queueLinks) { // ??? НЕ ФАКТ ЧТО НАДО
                            String linkString = innerLink.absUrl("href");
                            if (linkString.contains(SiteIndexingServiceImpl.getDomainName())
                                    & !visitedLinks.contains(linkString)
                                    && linkString.startsWith(link)
                                    && !isFile(linkString)) {
                                queueLinks.add(linkString);
                                ParserSiteUtil parserSiteUtil = new ParserSiteUtil(queueLinks, visitedLinks,
                                        siteRepository, pageRepository, siteModel, lastError, siteModelUtil,
                                        pageModelUtil, lemmaModelUtil, lemmaFinderUtil);
                                parserSiteUtil.fork();
                                siteModelUtil.updateSiteModel(siteModel, StatusSiteIndex.INDEXING,
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