package searchengine.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

@Repository
public interface PageRepository extends CrudRepository<PageModel, Integer> {
    PageModel findByPath(String path);
    List<PageModel> findAllPagesBySiteId(SiteModel siteModel);
    List<PageModel> findAll();
}