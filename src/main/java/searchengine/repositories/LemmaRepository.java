package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

import java.util.List;

public interface LemmaRepository extends CrudRepository<LemmaModel, Integer> {
//    Integer findMaxLemmaFrequencyBySiteId(SiteModel siteModel);
    Integer getMaxFrequencyBySiteId(Integer integer);
    List<LemmaModel> findAllByLemma(String word);
    List<LemmaModel> findAllLemmasBySiteId(SiteModel siteModel);
    LemmaModel findByLemmaAndSiteId(String lemma, SiteModel siteModel);
    LemmaModel findLemmaModelById(Integer integer);
}