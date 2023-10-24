package searchengine.services;

public interface IndexOnePageService {

    // проверка отношения страницы к сайту из списка и репозитория
    boolean indexOnePageByUrl(String webPageUrl);
}
