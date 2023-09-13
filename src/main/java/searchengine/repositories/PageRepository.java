package searchengine.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

@Repository
public interface PageRepository extends CrudRepository<PageModel, Integer> {
    Optional<PageModel> findByPath(String path);
    PageModel findBySiteId(Integer id);
    List<PageModel> findAllIdPagesBySiteId(SiteModel siteModel);
    void deleteAllDataById(Integer id);
}