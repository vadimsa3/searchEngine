package searchengine.model;

import org.springframework.stereotype.Component;

@Component
public enum StatusSiteIndex {
    INDEXING,
    INDEXED,
    FAILED;
}