package searchengine.services;

public interface SearchService {
    String performSearch(String query, String site, Integer offset, Integer limit);
}
