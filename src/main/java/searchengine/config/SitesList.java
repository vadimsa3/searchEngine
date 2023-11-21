package searchengine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode

@Component
@ConfigurationProperties(prefix = "indexing-settings")

public class SitesList {
    private List<Site> sites;
}
