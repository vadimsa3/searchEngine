package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
    SiteModel findSiteModelByUrl(String urlSite);
    List<SiteModel> findAll();
    List<SiteModel> findSiteModelsByUrl(String urlSite);
}