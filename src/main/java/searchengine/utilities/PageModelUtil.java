package searchengine.utilities;

import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;

import java.net.MalformedURLException;
import java.net.URL;

@Service
public class PageModelUtil {

    @Autowired
    private PageRepository pageRepository;

    public PageModel createNewPageModel(String url, Document document,
                                        SiteModel siteModel, Integer statusCode) {
        PageModel pageModel = new PageModel();
        pageModel.setSiteId(siteModel);
        pageModel.setPath(url.substring(siteModel.getUrl().length()));
        pageModel.setCode(statusCode);
        pageModel.setContent(document.outerHtml());
        pageRepository.save(pageModel);
        return pageModel;
    }
}