package searchengine.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteModel;

import java.util.List;

@Component
public interface SiteIndexingService {
    void startIndexingSite();
    boolean stopIndexingSite();
}
