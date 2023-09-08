package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


/* Controller обрабатывает запросы, возвращая представления (в данном случае — код веб-страницы).
Представление отвечает за отображение содержимого HTML
* */
@Controller
public class DefaultController {

    /**
     * При переходе на главную страницу
     * Метод формирует главную страницу из HTML- файла index.html (обрабатывает шаблон index.html),
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     * Автоматически подключает и возвращает в качестве ответа код одноимённой веб-страницы (index.html).
     */

    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
