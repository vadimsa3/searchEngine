package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Getter
/* Cпринг будет сканировать классы по спринговым аннотациям и создает бины по найденым классам (экземпляры класса).
 * */
@Component

// Чтобы данные из файла конфигурации попали в сервис, в приложении с реализованы классы Site и SiteList.

public class Site {
    private String url;
    private String name;
}
