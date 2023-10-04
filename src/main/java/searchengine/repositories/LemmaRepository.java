package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends CrudRepository<LemmaModel, Integer> {
    LemmaModel findByLemma(String lemma);
    List<LemmaModel> findAllLemmasBySiteId(SiteModel siteModel);
    LemmaModel findByLemmaAndSiteId(String lemma, SiteModel siteModel);
    void deleteAllLemmasById(Integer id);
}
