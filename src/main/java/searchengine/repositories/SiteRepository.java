package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
    SiteModel findSiteModelByUrl(String urlSite);
//    int countByStatus(StatusSiteIndex statusSiteIndex);
}
