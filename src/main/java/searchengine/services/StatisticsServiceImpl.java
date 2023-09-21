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
import searchengine.model.StatusSiteIndex;
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

    private final Random random = new Random(); // удалить после корректировки лемм
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {

//        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
//        String[] errors = {
//                "Ошибка индексации: главная страница сайта не доступна",
//                "Ошибка индексации: сайт не доступен",
//                ""
//        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false); // надо поработать с кнопкой (if (result.statistics.total.indexing))

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            SiteModel siteModel = siteRepository.findSiteModelByUrl(site.getUrl());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            long sitePages = random.nextInt(1_000); // ТО CORRECT !!!


//            // ----
//            long lemmas = lemmaRepository.count();
//            // ----

            long lemmas = sitePages * random.nextInt(1_000); // доработать на реальную цифру
//            item.setPages(sitePages);
//            item.setLemmas(lemmas);

            if (siteModel != null) {
                item.setStatus(siteModel.getStatusSiteIndex());
                item.setPages(sitePages);
                item.setLemmas(lemmas);
                item.setError(siteModel.getLastError() != null ? siteModel.getLastError() : "No errors found!");
                item.setStatusTime(siteModel.getStatusTime());
            } else {
                item.setStatus(StatusSiteIndex.FAILED);
                item.setPages(0);
                item.setLemmas(0);
                item.setError("Site not yet been indexed!");
                item.setStatusTime(LocalDateTime.now());
            }

            long allPages = pageRepository.count(); // общее кл-во страниц !! ИСПРАВИТЬ
            total.setPages(total.getPages() + allPages);
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
