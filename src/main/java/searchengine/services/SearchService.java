package searchengine.services;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface SearchService {
    String performSearch(String query, String site, Integer offset, Integer limit) throws IOException;
}
