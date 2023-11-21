package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


/* Controller обрабатывает запросы, возвращая представления (в данном случае — код веб-страницы).
Представление отвечает за отображение содержимого HTML
* */
@Controller
public class DefaultController {

    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
