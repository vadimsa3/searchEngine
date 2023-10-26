package searchengine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
/* Cпринг будет сканировать классы по спринговым аннотациям и создает бины по найденым классам (экземпляры класса).
 * */
@Component
/* Аннотации ConfigurationProperties приводят к автоматической инициализации объекта
этого класса данными из файла application.yaml.
prefix — это название ключа конфигурации, внутри которого лежит список сайтов.
* */
@ConfigurationProperties(prefix = "indexing-settings")

// Чтобы данные из файла конфигурации попали в сервис, в приложении с реализованы классы Site и SiteList.

public class SitesList {
    private List<Site> sites;
}
