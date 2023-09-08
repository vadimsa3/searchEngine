package searchengine.services;

import org.springframework.stereotype.Component;

@Component
public interface SiteIndexingService {
    void startIndexingSite();
}
