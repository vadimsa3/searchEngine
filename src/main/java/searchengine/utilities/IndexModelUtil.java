package searchengine.utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;

@Service
public class IndexModelUtil {

    @Autowired
    private IndexRepository indexRepository;

    public void createIndexModel(PageModel pageModel, LemmaModel lemmaModel, int count){
        IndexModel indexModel = new IndexModel();
        indexModel.setPageId(pageModel);
        indexModel.setLemmaId(lemmaModel);
        indexModel.setRank(count);
        indexRepository.save(indexModel);
    }
}