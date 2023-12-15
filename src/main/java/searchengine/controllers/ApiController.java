package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexOnePageService;
import searchengine.services.SearchService;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private SiteIndexingService siteIndexingService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private IndexOnePageService indexOnePageService;
    @Autowired
    private SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() throws IOException {
        boolean isUnsuccessfulResult = siteIndexingService.startIndexingSite();
        return isUnsuccessfulResult
                ? ResponseEntity.badRequest().body("{\"result\": false, \"error\":\""
                + "Индексация уже запущена" + "\"}")
                : ResponseEntity.ok().body("{\"result\": true}");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        boolean isActive = siteIndexingService.stopIndexingSite();
        return isActive
                ? ResponseEntity.ok().body("{\"result\": true}")
                : ResponseEntity.badRequest().body("{\"result\": false, \"error\":\""
                + "Индексация еще не запущена" + "\"}");
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam("url") String url) throws IOException {
        boolean isCorrect = indexOnePageService.indexOnePageByUrl(url);
        String errorMessage = "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле";
        return isCorrect
                ? ResponseEntity.ok().body("{\"result\": true}")
                : ResponseEntity.badRequest().body("{\"result\": false, \"error\":\"" + errorMessage + "\"}");
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam() String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) throws IOException {
        if (query == null || query.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"result\": false, \"error\": \"Задан пустой поисковый запрос\"}");
        }
        String jsonResult = searchService.beginSearch(query, site, offset, limit);
        if (jsonResult == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"result\": false, \"error\": \"Результаты по запросу не найдены\"}");
        }
        return ResponseEntity.status(HttpStatus.OK).body(jsonResult);
    }
}