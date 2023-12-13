package searchengine.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.PageData;
import searchengine.dto.search.SearchResult;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.WordFinderUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    PageRepository pageRepository;

    private Integer offset;
    private Integer limitOutputResults;
    private final LemmaFinderUtil lemmaFinderUtil;
    private final SearchResult searchResult;
    private final WordFinderUtil wordFinderUtil;
    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    public SearchServiceImpl() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
        wordFinderUtil = new WordFinderUtil();
        searchResult = new SearchResult();
    }

    @Override
    public String beginSearch(String query, String siteFromQuery, Integer offset,
                              Integer limitOutputResults) throws IOException {
        this.offset = offset;
        this.limitOutputResults = limitOutputResults;
        checkLanguageInputQuery(query);
        Map<String, Integer> uniqueLemmasFromQuery = lemmaFinderUtil.getLemmasMap(query);
        List<LemmaModel> listLemmaModelsFromRepository = getListLemmaModelsByLemmaFromRepository(uniqueLemmasFromQuery,
                siteFromQuery);
        log.info("В репозитории найдено соответствующих запросу лемм: " + listLemmaModelsFromRepository.size());
        Map<Integer, List<PageModel>> mapLemmasAndPages = getPagesByLemmaModel(listLemmaModelsFromRepository);
        Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages = getSortedLemmasByFrequencyOnPages(mapLemmasAndPages);
        List<IndexModel> matchingSearchIndexesModels = getListMatchingIndexes(sortedMapLemmasByFrequencyOnPages);
        Map<Integer, Double> pageRelevenceMap = calculateMaxPageRelevance(matchingSearchIndexesModels);
        List<PageData> pageDataList = createPageData(matchingSearchIndexesModels, sortedMapLemmasByFrequencyOnPages,
                pageRelevenceMap);
        pageDataList.sort((pageData1, pageData2) ->
                Double.compare(pageData2.getRelevance(), pageData1.getRelevance()));
        setSearchResult(pageDataList, matchingSearchIndexesModels.size());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(searchResult);
    }

    public Boolean checkLanguageInputQuery(String queryInput) {
        boolean result = true;
        String englishAlphabet = "[a-zA-z]+";
        if (queryInput.isEmpty()) {
            log.warn("Внимание! Запрос не введен. Необходимо ввести запрос на русском языке.");
            result = false;
        } else {
            String[] wordsFromQuery = queryInput.toLowerCase(Locale.ROOT).split("\\s+");
            for (String word : wordsFromQuery) {
                if (word.matches(englishAlphabet)) {
                    log.warn("Внимание! Необходимо сменить язык ввода на русский.");
                    result = false;
                }
            }
            log.info("Поисковый запрос: " + queryInput);
        }
        return result;
    }

    private List<LemmaModel> getListLemmaModelsByLemmaFromRepository(Map<String, Integer> uniqueLemmasFromQuery,
                                                                     String siteFromQuery) {
        List<LemmaModel> listLemmaModelsFromRepository = new ArrayList<>();
        for (String lemmaFromListQuery : uniqueLemmasFromQuery.keySet()) {
            if (siteFromQuery == null) {
                listLemmaModelsFromRepository.addAll(lemmaRepository.findAllByLemma(lemmaFromListQuery));
            } else {
                SiteModel siteModel = siteRepository.findSiteModelByUrl(siteFromQuery);
                LemmaModel lemmaModelFromRepository = lemmaRepository.findByLemmaAndSiteId(lemmaFromListQuery,
                        siteModel);
                listLemmaModelsFromRepository.add(lemmaModelFromRepository);
            }
        }
        return listLemmaModelsFromRepository;
    }

    private Map<Integer, List<PageModel>> getPagesByLemmaModel(List<LemmaModel> listLemmaModelsFromRepository) {
        Map<Integer, List<PageModel>> mapLemmasAndPages = new LinkedHashMap<>();
        listLemmaModelsFromRepository.forEach(lemmaModel -> {
            List<PageModel> listPageModels = indexRepository.findAllByLemmaId(lemmaModel).stream()
                    .map(IndexModel::getPageId)
                    .collect(Collectors.toList());
            if (listPageModels.size() == pageRepository.findAll().size()) {
                log.info("Лемма - " + lemmaModel.getLemma() + " - встречается на каждой странице сайта");
                mapLemmasAndPages.put(lemmaModel.getId(), listPageModels);
            } else {
                if (listPageModels.size() < pageRepository.findAll().size() * 60 / 100) {
                    mapLemmasAndPages.put(lemmaModel.getId(), listPageModels);
                } else {
                    log.info("Лемма - " + lemmaModel.getLemma() + " - встречается на слишком большом количестве страниц");
                }
            }
        });
        return mapLemmasAndPages;
    }

    private Map<Integer, List<PageModel>> getSortedLemmasByFrequencyOnPages(Map<Integer,
            List<PageModel>> mapLemmasAndPages) {
        return mapLemmasAndPages.entrySet()
                .stream()
                .sorted(Comparator.comparing(l -> l.getValue().size()))
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    private List<IndexModel> getListMatchingIndexes(Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages) {
        Set<IndexModel> tempMatchingIndexModels = new HashSet<>();
        List<IndexModel> matchingSearchIndexesModels = new ArrayList<>();
        if (!sortedMapLemmasByFrequencyOnPages.isEmpty()) {
            sortedMapLemmasByFrequencyOnPages.keySet().forEach(lemmaId -> {
                tempMatchingIndexModels.addAll(indexRepository.findAllByLemmaId(
                        lemmaRepository.findLemmaModelById(lemmaId)));
            });
        }
        if (!tempMatchingIndexModels.isEmpty()) {
            matchingSearchIndexesModels.addAll(tempMatchingIndexModels);
        }
        return matchingSearchIndexesModels;
    }

    private Map<Integer, Double> calculateMaxPageRelevance(List<IndexModel> matchingSearchIndexesModels) {
        Map<Integer, Double> pageRelevenceMap = new HashMap<>();
        for (IndexModel indexModel : matchingSearchIndexesModels) {
            double relevanceIndexModelOnPage = indexModel.getRank();
            int pageId = indexModel.getPageId().getId();
            if (pageRelevenceMap.containsKey(pageId)) {
                double currentPageRelevance = pageRelevenceMap.get(pageId);
                double newPageRelevance = currentPageRelevance + relevanceIndexModelOnPage;
                pageRelevenceMap.put(pageId, newPageRelevance);
            } else {
                pageRelevenceMap.put(pageId, relevanceIndexModelOnPage);
            }
        }
        return pageRelevenceMap;
    }

    private List<PageData> createPageData(List<IndexModel> matchingSearchIndexesModels,
                                          Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages,
                                          Map<Integer, Double> pageRelevenceMap) {
        List<PageData> pageDataResult = new ArrayList<>();
        double maxAbsoluteRelevance = Collections.max(pageRelevenceMap.values());
        int startOffset = Math.max(offset != null ? offset : 0, 0);
        int endOffset = Math.min(startOffset + (limitOutputResults != null &&
                limitOutputResults > 0 ? limitOutputResults : 20), matchingSearchIndexesModels.size());
        Set<Integer> uniquePageId = new HashSet<>();
        for (int i = startOffset; i < endOffset; i++) {
            int newPageId = matchingSearchIndexesModels.get(i).getPageId().getId();
            if (uniquePageId.add(newPageId)) {
                String site = matchingSearchIndexesModels.get(i).getPageId().getSiteId().getUrl();
                String siteName = matchingSearchIndexesModels.get(i).getPageId().getSiteId().getName();
                String uriPage = matchingSearchIndexesModels.get(i).getPageId().getPath();
                String fullContentPage = matchingSearchIndexesModels.get(i).getPageId().getContent();
                String title = wordFinderUtil.getTitleFromFullContentPage(fullContentPage);
                List<String> lemmas = convertLemmaIdToListLemmas(sortedMapLemmasByFrequencyOnPages);
                String snippet = wordFinderUtil.getSnippet(fullContentPage, lemmas);
                double absolutPageRelevance = pageRelevenceMap.getOrDefault(newPageId, 0.0);
                pageDataResult.add(setPageData(site, siteName, uriPage, title, snippet,
                        absolutPageRelevance, maxAbsoluteRelevance));
            }
        }
        return pageDataResult;
    }

    private PageData setPageData(String site, String siteName, String uri, String title, String snippet,
                                 Double absolutPageRelevance, Double maxAbsoluteRelevance) {
        PageData pageData = new PageData();
        pageData.setSite(site);
        pageData.setSiteName(siteName);
        pageData.setUri(uri);
        pageData.setTitle(title);
        pageData.setSnippet(snippet);
        pageData.setRelevance(absolutPageRelevance / maxAbsoluteRelevance);
        return pageData;
    }

    private List<String> convertLemmaIdToListLemmas(Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages) {
        List<String> lemmas = new ArrayList<>();
        sortedMapLemmasByFrequencyOnPages.keySet().forEach(lemmaId -> {
            lemmas.add(lemmaRepository.findLemmaModelById(lemmaId).getLemma());
        });
        return lemmas;
    }

    private void setSearchResult(List<PageData> pageDataList, int count) {
        searchResult.setData(pageDataList);
        searchResult.setCount(count);
        searchResult.setResult(true);
    }
}