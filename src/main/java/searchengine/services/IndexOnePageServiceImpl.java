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
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private PageModelUtil pageModelUtil;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;
    @Autowired
    private IndexModelUtil indexModelUtil;
    @Autowired
    private SiteModelUtil siteModelUtil;

    private static final Logger log = LoggerFactory.getLogger(IndexOnePageServiceImpl.class);
    private String urlSiteFromWebPageUrl;

    /*
    Проверьте работу индексации на отдельной странице, указав путь к ней в
    веб-интерфейсе вашего приложения и запустив её индексацию.

    Не забудьте, что при добавлении страницы в базу данных она должна
    привязываться к записи в таблице site, которая либо уже должна там
    находиться, либо должна быть создана на основе одного из пунктов
    списка сайтов в конфигурации вашего приложения.

    ● В случае попытки индексации страницы с какого-то другого сайта
    команда API должна выдавать ошибку в соответствии с технической
    спецификацией.
    Убедитесь в этом в веб-интерфейсе вашего приложения.

    ● В случае, если переданная страница уже была проиндексирована, перед
    её индексацией необходимо удалить всю информацию о ней из таблиц
    page, lemma и index.
     */

    @Override
    public boolean indexOnePageByUrl(String webPageUrl) {
        Site site = isListSitesContainsWebPageUrl(webPageUrl);
        boolean result;
        try {
            if (site.getUrl() == null | isEmptyWebPageUrl(webPageUrl)) {
                log.error("Данная страница находится за пределами сайтов," +
                        "указанных в конфигурационном файле");
                result = false;
            } else {
                SiteModel siteModel = getSiteModel(webPageUrl, site);
                log.info("Start indexing single page: " + webPageUrl);
                Connection.Response response = Jsoup.connect(webPageUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                        "Gecko/20100101 Firefox/25.0 Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41")
                        .referrer("http://www.google.com")
                        .timeout(3000)
                        .ignoreHttpErrors(true)
                        .execute();
                int statusCode = response.statusCode();
                Document document = response.parse();
                log.info("End indexing single page: " + webPageUrl);
                PageModel pageModel = saveNewOrUpdateOldPage(site, document, siteModel, statusCode, webPageUrl);
                saveNewOrUpdateOldLemma(pageModel, siteModel, document);
                log.info("Page indexing completed");
                result = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private boolean isEmptyWebPageUrl(String webPageUrl) {
        return webPageUrl.isBlank();
    }

    public SiteModel getSiteModel(String webPageUrl, Site site) {
        SiteModel siteModel = isSiteRepositoryContainsSiteModel(webPageUrl);
        if (siteModel == null) {
            log.info("Репозиторий пустой или модель сайта отсутствует в репозитории");
            siteModel = siteModelUtil.createNewSiteModel(site, StatusSiteIndex.INDEXING);
            log.info("В репозиторий добавлена новая модель сайта - "
                    + siteModel.getUrl() + " "
                    + siteModel.getStatusTime());
        }
        return siteModel;
    }

    public SiteModel isSiteRepositoryContainsSiteModel(String webPageUrl) {
        String urlSite = getUrlSiteFromWebPageUrl(webPageUrl);
        SiteModel siteModel = siteRepository.findSiteModelByUrl(urlSite);
        if (siteModel == null) {
            log.info("В репозитории отсутствует модель сайта с url - " + urlSite);
            return null;
        } else {
            log.info("В репозитории уже есть модель сайта с url - " + urlSite);
            return siteModel;
        }
    }

    public String getUrlSiteFromWebPageUrl(String webPageUrl) {
        Pattern pattern = Pattern.compile("http(s)?:\\/\\/(?:[-\\w]+\\.)?([-\\w]+)\\.\\w+(?:\\.\\w+)?");
        Matcher matcher = pattern.matcher(webPageUrl);
        while (matcher.find()) {
            urlSiteFromWebPageUrl = webPageUrl.substring(matcher.start(), matcher.end());
        }
        return urlSiteFromWebPageUrl;
    }

    private Site isListSitesContainsWebPageUrl(String webPageUrl) {
        String urlSiteFromWebPageUrl = getUrlSiteFromWebPageUrl(webPageUrl);
        Site newSite = new Site();
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().equals(urlSiteFromWebPageUrl)) {
                newSite = site;
            }
        }
        return newSite;
    }

    public PageModel isPageRepositoryContainsPageModel(String webPageUrl, SiteModel siteModel) {
        String path = webPageUrl.substring(siteModel.getUrl().length());
        return pageRepository.findByPath(path);
    }

    public PageModel saveNewOrUpdateOldPage(Site site, Document document, SiteModel siteModel,
                                            Integer statusCode, String webPageUrl) throws MalformedURLException {
        String path = webPageUrl.replaceAll(site.getUrl(), "");
        PageModel pageModel = isPageRepositoryContainsPageModel(webPageUrl, siteModel);
        if (pageModel != null) {
            pageModel.setSiteId(siteModel);
            pageModel.setPath(path);
            pageModel.setCode(statusCode);
            pageModel.setContent(document.outerHtml());
            pageRepository.save(pageModel);
            return pageModel;
        } else {
            return pageModelUtil.createNewPageModel(webPageUrl, document, siteModel, statusCode);
        }
    }

    public void saveNewOrUpdateOldLemma(PageModel pageModel, SiteModel siteModel, Document document) {
        String textPageForLemmasHtml = document.text();
        Map<String, Integer> lemmasMap = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasMap.keySet();
        for (String word : lemmasSet) {
            LemmaModel existingLemma = lemmaRepository.findByLemmaAndSiteId(word, siteModel);
            int countLemmaOnPage = lemmasMap.get(word);
            if (existingLemma != null) {
                int count = existingLemma.getFrequency() + countLemmaOnPage;
                existingLemma.setFrequency(count);
                lemmaRepository.save(existingLemma);
                indexModelUtil.createIndexModel(pageModel, existingLemma, count);
            } else {
                LemmaModel newLemmaModel = new LemmaModel();
                newLemmaModel.setSiteId(siteModel);
                newLemmaModel.setLemma(word);
                newLemmaModel.setFrequency(countLemmaOnPage);
                lemmaRepository.save(newLemmaModel);
                indexModelUtil.createIndexModel(pageModel, newLemmaModel, countLemmaOnPage);
            }
        }
    }
}