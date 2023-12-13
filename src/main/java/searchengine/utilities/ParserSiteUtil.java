package searchengine.utilities;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

import lombok.Getter;
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

    private final Queue<String> queueLinks;
    private final Set<String> visitedLinks;
    private final Map<Integer, String> lastError;

    @Getter
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

    protected void compute() {
        while (true) {
            String link = queueLinks.poll();
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
                    int statusCode = getResponse(link).statusCode();
                    Document document = getResponse(link).parse();
                    PageModel pageModel = pageModelUtil.createNewPageModel(link, document, siteModel, statusCode);
                    lemmaModelUtil.createNewLemmaModel(pageModel, siteModel);
                    Elements urls = document.getElementsByTag("a");
                    urls.forEach((innerLink) -> {
                        synchronized (queueLinks) {
                            String linkString = innerLink.absUrl("href");
                            if (checkLink(linkString, link)) {
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

    private boolean checkLink(String linkString, String link) {
        return linkString.contains(SiteIndexingServiceImpl.getDomainName())
                & !visitedLinks.contains(linkString)
                && linkString.startsWith(link)
                && !isFile(linkString);
//                && !linkString.contains("#");
    }

    private static boolean isFile(String link) {
        String reg = "([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf|zip|jar|ppt|pptx))$)";
        return link.matches(reg);
    }

    public Connection.Response getResponse(String link) throws IOException {
        return Jsoup.connect(link)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                .referrer("http://google.com")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .execute();
    }
}