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

    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteIndexingService siteIndexingService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private IndexOnePageService indexOnePageService;
    @Autowired
    private SearchService searchService;

    /*  Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.
    Если ошибок индексации того или иного сайта нет, задавать ключ error не нужно.
    */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /*Запуск сервиса полной индексации — GET /api/startIndexing
    Метод запускает полную индексацию всех сайтов или полную переиндексацию, если они уже проиндексированы.
    Если в настоящий момент индексация или переиндексация уже запущена, возвращает соответствующее сообщение об ошибке.
    */
    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() throws IOException {
        boolean isIndexing = siteIndexingService.startIndexingSite();
        return !isIndexing
                ? ResponseEntity.badRequest().body("{\"result\": false, \"error\":\""
                + "Indexing has already started" + "\"}")
                : ResponseEntity.ok().body("{\"result\": true}");
    }

    /*Остановка текущей индексации — GET /api/stopIndexing
    Метод останавливает текущий процесс индексации (переиндексации).
    Если в настоящий момент индексация или переиндексация не происходит, возвращает соответствующее сообщение об ошибке.
    */
    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        boolean isActive = siteIndexingService.stopIndexingSite();
        return isActive
                ? ResponseEntity.ok().body("{\"result\": true}")
                : ResponseEntity.badRequest().body("{\"result\": false, \"error\":\""
                + "Индексация еще не запущена" + "\"}");
    }

    /*Добавление или обновление отдельной страницы — POST /api/indexPage
    Метод добавляет в индекс или обновляет отдельную страницу, адрес которой передан в параметре.
    Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.
    Параметры: url — адрес страницы, которую нужно переиндексировать.
    * */
    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam("url") String url) throws IOException {
        System.out.println("Начало индексации одной страницы - потом стереть из контроллера " + url); // потом убрать
        boolean isCorrect = indexOnePageService.indexOnePageByUrl(url);
        String errorMessage = "Данная страница находится за пределами сайтов," +
                "указанных в конфигурационном файле";
        return isCorrect
                ? ResponseEntity.ok().body("{\"result\": true}")
                : ResponseEntity.badRequest().body("{\"result\": false, \"error\":\"" + errorMessage + "\"}");
    }

    /*Получение данных по поисковому запросу — GET /api/search
    Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).

    Параметры:
    ●	query — поисковый запрос;
    ●	site — сайт, по которому осуществлять поиск (если не задан, поиск должен происходить по всем проиндексированным
    сайтам); задаётся в формате адреса, например: http://www.site.com (без слэша в конце);
    ●	offset — сдвиг от 0 для постраничного вывода (параметр необязательный; если не установлен,
    то значение по умолчанию равно нулю);
    ●	limit — количество результатов, которое необходимо вывести (параметр необязательный;
    если не установлен, то значение по умолчанию равно 20).
    * */

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
        } else {
            String jsonResult = searchService.beginSearch(query, site, offset, limit);
            return ResponseEntity.status(HttpStatus.OK).body(jsonResult);
        }
    }
}

