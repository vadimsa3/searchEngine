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

        /* 1. Разбиваем поисковый запрос на отдельные слова и формируем из этих слов список уникальных лемм,
        исключая междометия, союзы, предлоги и частицы + добавим их количество*/
        Map<String, Integer> uniqueLemmasFromQuery = lemmaFinderUtil.getLemmasMap(query);
        System.out.println("СПИСОК ЛЕММ ИЗ ПОИСКОВОГО ЗАПРОСА С КОЛИЧЕСТВОМ :" + uniqueLemmasFromQuery); // удалить

        /* 1.1. По леммам из запроса и сайту, находим все модели-лемм из репозитория*/
        List<LemmaModel> listLemmaModelsFromRepository = getListLemmaModelsByLemmaFromQuery(uniqueLemmasFromQuery,
                siteFromQuery);

        /* 1.2. По найденным моделям-леммам, находим все страницы, на которых она встречается.*/
        /* 2. Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
        Поэкспериментируйте и определите этот процент самостоятельно.*/
        Map<Integer, List<PageModel>> mapLemmasAndPages = getPagesByLemmaModel(listLemmaModelsFromRepository);

        /* 3. Сортировать леммы в порядке увеличения частоты встречаемости на страницах (от самых редких до самых частых).
         сортируем в порядке встречаемости на найденных страницах
        /*4. По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается и т.д..*/
        Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages = getSortedLemmasByFrequencyOnPages(mapLemmasAndPages);

        // ищем по репозиторию индексов страницы, соответствующие лемме
        // сохраняем перечень моделей индексов в set
        List<IndexModel> matchingSearchIndexesModels = getListMatchingIndexes(sortedMapLemmasByFrequencyOnPages);

        /* 5. Если страницы найдены, рассчитывать по каждой из них релевантность (и выводить её потом) и возвращать.*/
        Map<Integer, Double> pageRelevenceMap = calculateMaxPageRelevance(matchingSearchIndexesModels);

        /*6. Список объектов страниц с учетом полученных данных. В т.ч. абсолютной и относительной релевантностей*/
        List<PageData> pageDataList = setPageData(matchingSearchIndexesModels, sortedMapLemmasByFrequencyOnPages,
                pageRelevenceMap);

        /*7. Сортировать страницы по убыванию релевантности (от большей к меньшей)*/
        pageDataList.sort((pageData1, pageData2) ->
                Double.compare(pageData2.getRelevance(), pageData1.getRelevance()));
        // и выдавать в виде списка объектов
        setSearchResult(pageDataList, matchingSearchIndexesModels.size());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(searchResult);
    }

    //--------------------------------------------------------------------------------------------------------------
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
                } else {
                    log.info("Поисковый запрос: " + queryInput);
                }
            }
        }
        return result;
    }

    //--------------------------------------------------------------------------------------------------------------
    /* 1.1. По леммам из запроса, находим все модели лемм из репозитория с учетом модели сайта.*/
    private List<LemmaModel> getListLemmaModelsByLemmaFromQuery(Map<String, Integer> uniqueLemmasFromQuery,
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

    //--------------------------------------------------------------------------------------------------------------
    /* 1.2. По моделям лемм, находим все страницы, на которых она встречается.
     * 2. Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
     * Оставляем леммы, с встречаемостью менее 60% от общего количества страниц.*/
    private Map<Integer, List<PageModel>> getPagesByLemmaModel(List<LemmaModel> listLemmaModelsFromRepository) {
        Map<Integer, List<PageModel>> mapLemmasAndPages = new LinkedHashMap<>();
        listLemmaModelsFromRepository.forEach(lemmaModel -> {
            List<PageModel> listPageModels = indexRepository.findAllByLemmaId(lemmaModel).stream()
                    .map(IndexModel::getPageId)
                    .collect(Collectors.toList());
            if (listPageModels.size() < pageRepository.findAll().size() * 60 / 100) {
                mapLemmasAndPages.put(lemmaModel.getId(), listPageModels);
                System.out.println("По моделям лемм, находим все страницы: " + "Lemma - " +
                        lemmaModel.getLemma() + ". " + "ID Lemma - " + lemmaModel.getId() + ". "
                        + "Count lemmas - " + " - " + listPageModels.size()); // удалить потом
            }
        });
        return mapLemmasAndPages;
    }

    //--------------------------------------------------------------------------------------------------------------
    /* 3. Сортировать леммы в порядке увеличения частоты встречаемости на страницах (от самых редких до самых частых).
     * сортируем в порядке встречаемости на найденных страницах*/
    private Map<Integer, List<PageModel>> getSortedLemmasByFrequencyOnPages(Map<Integer,
            List<PageModel>> mapLemmasAndPages) {
        return mapLemmasAndPages.entrySet()
                .stream()
                .sorted(Comparator.comparing(l -> l.getValue().size()))
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    //--------------------------------------------------------------------------------------------------------------
    /* Ищем по репозиторию индексов страницы, соответствующие лемме из сортированного списка
    и сохраняем список моделей индексов*/
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

    //--------------------------------------------------------------------------------------------------------------
    /* 5. Если страницы найдены, рассчитывать по каждой из них релевантность*/
    private Map<Integer, Double> calculateMaxPageRelevance(List<IndexModel> matchingSearchIndexesModels) {
        Map<Integer, Double> pageRelevenceMap = new HashMap<>();
        for (IndexModel indexModel : matchingSearchIndexesModels) {
            double relevanceOnPage = indexModel.getRank(); // получаем релевантность - amount каждой indexModel
            int pageId = indexModel.getPageId().getId();
            if (pageRelevenceMap.containsKey(pageId)) {
                double currentPageRelevance = pageRelevenceMap.get(pageId);
                double newPageRelevance = currentPageRelevance + relevanceOnPage;
                pageRelevenceMap.put(pageId, newPageRelevance);
            } else {
                pageRelevenceMap.put(pageId, relevanceOnPage);
            }
        }
        return pageRelevenceMap;
    }

    //--------------------------------------------------------------------------------------------------------------
    private List<PageData> setPageData(List<IndexModel> matchingSearchIndexesModels,
                                       Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages,
                                       Map<Integer, Double> pageRelevenceMap) {
        List<PageData> pageDataResult = new ArrayList<>();
        double maxAbsoluteRelevance = Collections.max(pageRelevenceMap.values());
        /* offset — сдвиг от 0 для постраничного вывода
        (параметр необязательный, если не установлен, то значение по умолчанию равно нулю)*/
        int startOffset = Math.max(offset != null ? offset : 0, 0);
        /* limit — количество результатов, которое необходимо вывести
        (параметр необязательный, если не установлен, то значение по умолчанию равно 20).*/
        int endOffset = Math.min(startOffset + (limitOutputResults != null &&
                limitOutputResults > 0 ? limitOutputResults : 20), matchingSearchIndexesModels.size());
        Set<Integer> uniquePageId = new HashSet<>();
        for (int i = startOffset; i < endOffset; i++) {
            int newPageId = matchingSearchIndexesModels.get(i).getPageId().getId();
            if (uniquePageId.add(newPageId)) {
                String site = matchingSearchIndexesModels.get(i).getPageId().getSiteId().getUrl();
                String siteName = matchingSearchIndexesModels.get(i).getPageId().getSiteId().getName();

                // !!! ЧТО-ТО С АДРЕСОМ
                String url = matchingSearchIndexesModels.get(i).getPageId().getPath();
                System.out.println(" ++++ ЧТО-ТО С АДРЕСОМ ++++ " + url); // delete

                String fullContentPage = matchingSearchIndexesModels.get(i).getPageId().getContent();
                String title = wordFinderUtil.getTitleFromFullContentPage(fullContentPage);

                List<String> lemmas = convertLemmaIdToListLemmas(sortedMapLemmasByFrequencyOnPages);

//                String snippet = wordFinderUtil.getSnippet(fullContentPage, lemmas);
                List<String> snippet = wordFinderUtil.getSnippet(fullContentPage, lemmas);

                PageData pageData = new PageData();
                double absolutPageRelevance = pageRelevenceMap.getOrDefault(newPageId, 0.0);
                pageData.setSite(site);
                pageData.setSiteName(siteName);
                pageData.setUrl(url);
                pageData.setTitle(title);
//                pageData.setSnippet(snippet);
                pageData.setSnippet(snippet);
                pageData.setRelevance(absolutPageRelevance / maxAbsoluteRelevance);
                pageDataResult.add(pageData);
            }
        }
        return pageDataResult;
    }

    //--------------------------------------------------------------------------------------------------------------
    private List<String> convertLemmaIdToListLemmas(Map<Integer, List<PageModel>> sortedMapLemmasByFrequencyOnPages) {
        List<String> lemmas = new ArrayList<>();
        sortedMapLemmasByFrequencyOnPages.keySet().forEach(lemmaId -> {
            lemmas.add(lemmaRepository.findLemmaModelById(lemmaId).getLemma());
        });
        return lemmas;
    }

    //--------------------------------------------------------------------------------------------------------------
    // выдавать в виде списка объектов
    /*7. Сортировать страницы по убыванию релевантности (от большей к меньшей)*/
//        pageDataList.sort((pageData1, pageData2) ->
//            Double.compare(pageData2.getRelevance(), pageData1.getRelevance()));
    private void setSearchResult(List<PageData> pageDataList, int count) {
        searchResult.setData(pageDataList);
        searchResult.setCount(count);
        searchResult.setResult(true);
    }
}