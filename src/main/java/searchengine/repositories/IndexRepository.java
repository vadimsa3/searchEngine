package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;

import java.util.List;

public interface IndexRepository extends CrudRepository<IndexModel, Integer> {
    List<IndexModel> findAllByLemmaId(LemmaModel lemmaModel);
}
