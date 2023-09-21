package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// тело ответа будет конвертироваться в JSON формат
// запрос будет ожидать JSON в теле запроса
@RestController
// создаст конструктор с инициализацией добавляемых сервисов (чтобы не писать конструктор в коде)
// загонит в конструктор все final поля
@RequiredArgsConstructor
// т.к. все запросы в этом классе будут начинаться с /api
@RequestMapping("/api")
public class ApiController {

    // автоматическое создание и подключения репозиториев
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    // автоматическое создание и подключение сервисов
    @Autowired
    private SiteIndexingService siteIndexingService;
    @Autowired
    private StatisticsService statisticsService;

    /* !!!!!! Контроллер должен только получать данные от пользователя и вызывать нужный сервис.
    Все расчеты и проверки должны быть в классах сервисах. !!!!!!!!
    */

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
    public ResponseEntity<String> startIndexing() throws IOException {
        boolean isIndexing = siteIndexingService.startIndexingSite();
        if (isIndexing) {
            String errorMessage = "Indexing has already started";
            return ResponseEntity.badRequest().body("{\"result\": false, \"error\":\"" + errorMessage + "\"}");
        } else {
            return ResponseEntity.ok().body("{\"result\": true}");
        }
    }

    /*Остановка текущей индексации — GET /api/stopIndexing
    Метод останавливает текущий процесс индексации (переиндексации).
    Если в настоящий момент индексация или переиндексация не происходит, возвращает соответствующее сообщение об ошибке.
    */
    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        boolean isActive = siteIndexingService.stopIndexingSite();
        if (isActive) {
            return ResponseEntity.ok().body("{\"result\": true}");
        } else {
            String errorMessage = "Indexing is not running";
            return ResponseEntity.badRequest().body("{\"result\": false, \"error\":\"" + errorMessage + "\"}");
        }
    }
}

//
//            Map<String, String> response = new HashMap<>();
//            SitesList sitesList = new SitesList();
//            for(Site site : sitesList.getSites()){
//                isIndexing = fjp.invoke(new IndexingService(siteRepositories,site,pageRepositories));
//            }
//            if(isIndexing){
//                response.put("result",isIndexing.toString());
//                return new ResponseEntity<>(response,HttpStatus.OK);
//            }else {
//                response.put("result",isIndexing.toString());
//                response.put("error","Индексация уже запущена");
//                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        }
//
//        task.setDone(true); // статус выполнение задачи
//        task.setCreationTime(LocalDateTime.now()); // дата внесения задачи
//        DBSiteRepository.save(site);
//        return ResponseEntity<>(HttpStatus.CREATED); // статус 201
//    }


/*Добавление или обновление отдельной страницы — POST /api/indexPage
Метод добавляет в индекс или обновляет отдельную страницу, адрес которой передан в параметре.
Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.
Параметры: url — адрес страницы, которую нужно переиндексировать.

@PostMapping("/indexPage")
    public void indexPage(@RequestParam String url){
    }
* */

/*Получение данных по поисковому запросу — GET /api/search
Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
Чтобы выводить результаты порционно, также можно задать параметры offset (сдвиг от начала списка результатов)
и limit (количество результатов, которое необходимо вывести).
В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit,
и массив data с результатами поиска.
Каждый результат — это объект, содержащий свойства результата поиска (см. ниже структуру и описание каждого свойства).
Если поисковый запрос не задан или ещё нет готового индекса
(сайт, по которому ищем, или все сайты сразу не проиндексированы), метод должен вернуть соответствующую ошибку
(см. ниже пример). Тексты ошибок должны быть понятными и отражать суть ошибок.
Параметры:
●	query — поисковый запрос;
●	site — сайт, по которому осуществлять поиск (если не задан, поиск должен происходить по всем проиндексированным
сайтам); задаётся в формате адреса, например: http://www.site.com (без слэша в конце);
●	offset — сдвиг от 0 для постраничного вывода (параметр необязательный; если не установлен,
то значение по умолчанию равно нулю);
●	limit — количество результатов, которое необходимо вывести (параметр необязательный;
если не установлен, то значение по умолчанию равно 20).
* */

