package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
        SiteModel findByUrl(String url);
    }
