package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;

import java.util.List;

public interface IndexRepository extends CrudRepository<IndexModel, Integer> {
    List<IndexModel> findByLemmaId(LemmaModel lemmaModel);
    List<IndexModel> findByPageId(PageModel pageModel);
}
