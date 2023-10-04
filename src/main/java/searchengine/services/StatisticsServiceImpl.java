package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.PageModel;
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
    @Autowired
    private SiteIndexingService siteIndexingService;

    private final Random random = new Random(); // удалить после корректировки лемм
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(siteIndexingService.isIndexing());

        // При нажатии StartIndexing, должен прийти ответ в формате, который указан в тз.
        // Если он true, то кнопка Start Indexing меняется на StopIndexing, если false - то отображается ошибка,
        // которая пришла в ответе

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            SiteModel siteModel = siteRepository.findSiteModelByUrl(site.getUrl());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setStatus(siteModel != null ? siteModel.getStatusSiteIndex() : StatusSiteIndex.FAILED);
            item.setPages(siteModel != null ? pageRepository.findAllPagesBySiteId(siteModel).size() : 0);
            item.setLemmas(siteModel != null ? lemmaRepository.findAllLemmasBySiteId(siteModel).size() : 0);
            item.setStatusTime(siteModel != null ? siteModel.getStatusTime() : LocalDateTime.now());
            item.setError(siteModel != null ? (siteModel.getLastError() != null ? siteModel.getLastError()
                    : "No errors found!") : "Site not yet been indexed!");
            long allPages = pageRepository.count();
            total.setPages(allPages);
            long allLemmas = lemmaRepository.count();
            total.setLemmas(allLemmas);
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
