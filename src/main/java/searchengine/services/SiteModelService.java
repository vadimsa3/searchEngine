package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
public class SiteModelService {

    @Autowired
    private SiteRepository siteRepository;

    public SiteModel createSiteModel(Site site) {
        SiteModel siteModel = new SiteModel();
        siteModel.setStatusSiteIndex(StatusSiteIndex.INDEXING);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setLastError("lastError"); // РАЗОБРАТЬСЯ
//        siteModel.setLastError(lastError.get(siteModel.getId()));
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteRepository.save(siteModel);
        return siteModel;
    }

    public void updateSiteModel(SiteModel siteModel, StatusSiteIndex statusSiteIndex,
                                 LocalDateTime timeStatus, String lastError) {
        siteModel.setStatusSiteIndex(statusSiteIndex);
        siteModel.setStatusTime(timeStatus);
        siteModel.setLastError(lastError);
        siteRepository.save(siteModel);
    }

    public void updateStatusSiteModelToFailed(SiteModel siteModel, StatusSiteIndex statusSiteIndex,
                                              LocalDateTime timeStatus, String errorMessage) {
        siteModel.setStatusSiteIndex(statusSiteIndex);
        siteModel.setStatusTime(timeStatus);
        siteModel.setLastError(errorMessage);
        siteRepository.save(siteModel);
    }
}
