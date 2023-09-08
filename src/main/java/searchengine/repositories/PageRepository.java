package searchengine.repositories;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

@Repository
public interface PageRepository extends CrudRepository<PageModel, Integer> {
    Optional<PageModel> findByPath(String path);
    PageModel findBySiteId(Integer id);
}