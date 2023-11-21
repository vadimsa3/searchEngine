package searchengine.services;

import org.springframework.stereotype.Component;

@Component
public interface SiteIndexingService {
    boolean startIndexingSite();
    boolean stopIndexingSite();
    boolean isIndexing();
}