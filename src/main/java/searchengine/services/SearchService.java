package searchengine.services;

import java.io.IOException;

public interface SearchService {
    String beginSearch(String query, String site, Integer offset, Integer limit) throws IOException;
}
