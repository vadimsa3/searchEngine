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
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.*;

import java.io.IOException;
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

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);
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
        System.out.println("!!! Сайт для дальнейшей работы - " + site.getName() + " " + site.getUrl());
        boolean result;
        try {
            if (site.getUrl() == null | isEmptyWebPageUrl(webPageUrl)) {
                log.error("Данная страница находится за пределами сайтов," +
                        "указанных в конфигурационном файле");
                result = false;
            } else {
                SiteModel siteModel = getSiteModel(webPageUrl, site);
                System.out.println("SITE_MODEL для продолжения= " + siteModel.getUrl());
                log.info("Start indexing single page: " + webPageUrl);
                Connection.Response response =
                        Jsoup.connect(webPageUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:25.0) " +
                                        "Gecko/20100101 Firefox/25.0")
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
        System.out.println("Переданный адрес isEmpty ? " + webPageUrl.isBlank());
        return webPageUrl.isBlank();
    }

    /* проверка на наличие в репозитории сайта, вернет null если не найдет или SiteModel
    проверяем по
        если модель сайта есть в репозитории:
     - ничего с моделью сайта не делаем,
     - проверяем на наличие в репозитории страниц наличие страницы по path,
        если есть - обновляем в ней данные (или удаляем в ней данные - должны удалится связанные леммы и индексы +
     парсим страницу и создаем новую с id сайта, модель которого нашли)
        если нет - парсим страницу и создаем новую с id сайта, модель которого нашли.

        если модели сайта нет в репозитории:
     - проверяем на наличие записи с переданным weburl в списке конфигурации,
        если есть - создаем новую модель сайта c weburl, парсим страницу, создаем страницу, леммы, индексы.
        если нет - выводим сообщение о не нахождении в списке.
     */

    public SiteModel getSiteModel(String webPageUrl, Site site) {
        System.out.println("-------- Начало работы getSiteModel ------------");
        SiteModel siteModel = isSiteRepositoryContainsSiteModel(webPageUrl);
        if (siteModel == null) {
            log.info("Репозиторий пустой или модель сайта отсутствует в репозитории");
            siteModel = siteModelUtil.createNewSiteModel(site);
            log.info("В репозиторий добавлена новая модель сайта - "
                    + siteModel.getUrl() + " "
                    + siteModel.getStatusTime());
        }
        // получаем модель из репозитория
        System.out.println("Все ОК берем модель сайта дальше - "
                + siteModel.getName() + "  "
                + siteModel.getStatusTime());
        return siteModel;
    }

    public SiteModel isSiteRepositoryContainsSiteModel(String webPageUrl) {
        System.out.println("-------- Начало работы isSiteRepositoryContainsSiteModel ------------");
        String urlSite = getUrlSiteFromWebPageUrl(webPageUrl);
        System.out.println("URL сайта для поиска в репозитории " + urlSite);
        SiteModel siteModel = siteRepository.findSiteModelByUrl(urlSite);
        if (siteModel == null) {
            log.info("В репозитории отсутствует модель сайта с url - " + urlSite);
            return null;
        } else {
            log.info("В репозитории уже есть модель сайта с url - " + urlSite);
            System.out.println("ВОТ ОНА " + siteModel.getName() + "  " + siteModel.getStatusTime());
            return siteModel;
        }
    }

    public String getUrlSiteFromWebPageUrl(String webPageUrl) {
        System.out.println("-------- Начало работы getUrlSiteFromWebPageUrl ------------");
        Pattern pattern = Pattern.compile("http(s)?:\\/\\/(?:[-\\w]+\\.)?([-\\w]+)\\.\\w+(?:\\.\\w+)?");
        Matcher matcher = pattern.matcher(webPageUrl);
        while (matcher.find()) {
            urlSiteFromWebPageUrl = webPageUrl.substring(matcher.start(), matcher.end());
        }
        System.out.println("URL сайта для дальнейшей работы from getUrlSiteFromWebPageUrl " + urlSiteFromWebPageUrl);
        return urlSiteFromWebPageUrl;
    }

    private Site isListSitesContainsWebPageUrl(String webPageUrl) {
        System.out.println("-------- Начало работы isListSitesContainsWebPageUrl ------------");
        String urlSiteFromWebPageUrl = getUrlSiteFromWebPageUrl(webPageUrl);
        Site newSite = new Site();
        for (Site site : sitesList.getSites()) {
            System.out.println("Сравниваем с сайтом - " + site.getUrl());
            System.out.println("РЕЗУЛЬТАТ по сайту " + site.getUrl() + " - " + site.getUrl().equals(urlSiteFromWebPageUrl));
            if (site.getUrl().equals(urlSiteFromWebPageUrl)) {
                newSite = site;
            }
        }
        System.out.println("СООТВЕТСТВУЕТ МОДЕЛИ САЙТА " + newSite);
        return newSite;
    }

    public PageModel isPageRepositoryContainsPageModel(String webPageUrl, SiteModel siteModel) {
        System.out.println("-------- Начало работы isPageRepositoryContainsPageModel ------------");
        String path = webPageUrl.substring(siteModel.getUrl().length());
        System.out.println("Найдена модель страницы в репозитории ? " + pageRepository.findByPath(path));
        return pageRepository.findByPath(path);
    }

    public PageModel saveNewOrUpdateOldPage(Site site, Document document, SiteModel siteModel,
                                            Integer statusCode, String webPageUrl) {
        System.out.println("-------- Начало работы saveNewOrUpdateOldPage ------------");
        String path = webPageUrl.replaceAll(site.getUrl(), "");
        PageModel pageModel = isPageRepositoryContainsPageModel(webPageUrl, siteModel);
        if (pageModel != null) {
            System.out.println("В PageRepository уже есть модель страницы " + pageModel.getPath());
            pageModel.setSiteId(siteModel);
            pageModel.setPath(path);
            pageModel.setCode(statusCode);
            pageModel.setContent(document.outerHtml());
            pageRepository.save(pageModel);
            System.out.println("PageModel обновлена");
            return pageModel;
        } else {
            System.out.println("В PageRepository нет модели, создана новая PageModel");
            return pageModelUtil.createNewPageModel(webPageUrl, document, siteModel, statusCode);
        }
    }

    public void saveNewOrUpdateOldLemma(PageModel pageModel, SiteModel siteModel, Document document) {
        System.out.println("-------- Начало работы saveNewOrUpdateOldLemma ------------");
        String textPageForLemmasHtml = document.text();
        Map<String, Integer> lemmasMap = lemmaFinderUtil.getLemmasMap(textPageForLemmasHtml);
        Set<String> lemmasSet = lemmasMap.keySet();
        for (String word : lemmasSet) {
            LemmaModel existingLemma = lemmaRepository.findByLemmaAndSiteId(word, siteModel);
            int countLemma = lemmasMap.get(word);
            if (existingLemma != null) {
                int count = existingLemma.getFrequency() + countLemma;
                existingLemma.setFrequency(count);
                lemmaRepository.save(existingLemma);
                indexModelUtil.createIndexModel(pageModel, existingLemma, count);
            } else {
                LemmaModel newLemmaModel = new LemmaModel();
                newLemmaModel.setSiteId(siteModel);
                newLemmaModel.setLemma(word);
                newLemmaModel.setFrequency(countLemma);
                lemmaRepository.save(newLemmaModel);
                indexModelUtil.createIndexModel(pageModel, newLemmaModel, countLemma);
            }
        }
    }
}