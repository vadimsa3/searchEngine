package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    private final Random random = new Random();
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {

        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false); // надо поработать с кнопкой (if (result.statistics.total.indexing))

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            SiteModel siteModel = siteRepository.findSiteModelByUrl(site.getUrl());

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName()); // OK
            item.setUrl(site.getUrl());   // OK

            // ----
            long pages = pageRepository.count(); // общее кл-во страниц !! ИСПРАВИТЬ
            // ----

//            int pages = random.nextInt(1_000); // доработать на реальную цифру

//            // ----
//            long lemmas = lemmaRepository.count();
//            // ----

            long lemmas = pages * random.nextInt(1_000); // доработать на реальную цифру
            item.setPages(pages); // OK
            item.setLemmas(lemmas); // OK
            item.setStatus(siteModel.getStatusSiteIndex()); // OK
            item.setError(siteModel.getLastError() != null ? siteModel.getLastError() : "No errors found!"); // OK
            item.setStatusTime(siteModel.getStatusTime());

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
