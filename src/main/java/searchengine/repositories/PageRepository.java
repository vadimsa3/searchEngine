package searchengine.repositories;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

@Repository
public interface PageRepository extends CrudRepository<PageModel, Integer> {
    Optional<PageModel> findPageByPath(String path);
    void deleteAllDataById(Integer id);
    void deletePageModelByPath(String path);
    List<PageModel> findAllPagesBySiteId(SiteModel siteModel);
}