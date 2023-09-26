package searchengine.services;

import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;

@Service
public class PageModelService {

    @Autowired
    private PageRepository pageRepository;

    public void createPageModel(String url, Document document, SiteModel siteModel, Integer statusCode) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteId(siteModel);
        pageModel.setPath(url.substring(siteModel.getUrl().length()));
        pageModel.setCode(statusCode);
        pageModel.setContent(document.outerHtml());
        pageRepository.save(pageModel);
    }
}
