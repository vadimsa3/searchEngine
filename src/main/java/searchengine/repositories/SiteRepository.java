package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
    SiteModel findSiteModelByUrl(String urlSite);

    //    int countByStatus(StatusSiteIndex statusSiteIndex);
//    List<SiteModel> findAllSitesBySiteStatus(StatusSiteIndex statusSiteIndex);
}
