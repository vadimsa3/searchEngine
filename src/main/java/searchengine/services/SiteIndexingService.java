package searchengine.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.SiteModel;

import java.util.List;

@Component
public interface SiteIndexingService {
    boolean startIndexingSite();
    boolean stopIndexingSite();
    boolean startIndexSingleSite(Site site);
}
