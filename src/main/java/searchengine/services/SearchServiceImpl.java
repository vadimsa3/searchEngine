package searchengine.services;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import searchengine.repositories.SiteRepository;
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.WordFinderUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


        /*Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
        Чтобы выводить результаты порционно, также можно задать параметры
        offset (сдвиг от начала списка результатов)
        и limit (количество результатов, которое необходимо вывести).
        В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit,
        и массив data с результатами поиска.
        Каждый результат — это объект, содержащий свойства результата поиска
        (см. ниже структуру и описание каждого свойства).
        Если поисковый запрос не задан или ещё нет готового индекса
        (сайт, по которому ищем, или все сайты сразу не проиндексированы),
        метод должен вернуть соответствующую ошибку
        (см. ниже пример). Тексты ошибок должны быть понятными и отражать суть ошибок.*/

        /*Метод должен выполнять следующий алгоритм:
        ● 1. Разбивать поисковый запрос на отдельные слова и формировать из этих слов список уникальных лемм,
        исключая междометия, союзы, предлоги и частицы. Используйте для этого код,
        который вы уже писали в предыдущем этапе.
        ● 2. Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
        Поэкспериментируйте и определите этот процент самостоятельно.
        ● 3. Сортировать леммы в порядке увеличения частоты встречаемости (по возрастанию значения поля frequency)
        — от самых редких до самых частых.
        ● 4. По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.
        Далее искать соответствия следующей леммы из этого списка страниц, а затем повторять операцию
        по каждой следующей лемме. Список страниц при этом на каждой итерации должен уменьшаться.
        ● Если в итоге не осталось ни одной страницы, то выводить пустой
        список.
        ● Если страницы найдены, рассчитывать по каждой из них
        релевантность (и выводить её потом, см. ниже) и возвращать.
        ● Для каждой страницы рассчитывать абсолютную релевантность —
        сумму всех rank всех найденных на странице лемм (из таблицы
        index), которая делится на максимальное значение этой
        абсолютной релевантности для всех найденных страниц. Пример
        расчёта:*/

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;

    // offset (сдвиг от начала списка результатов)
    private Integer offset;
    // limit (количество результатов, которое необходимо вывести)
    private Integer limitOutputResults;
    private final LemmaFinderUtil lemmaFinderUtil;
    private final SearchResult searchResult;
    private final WordFinderUtil wordFinderUtil;

    public SearchServiceImpl() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
        wordFinderUtil = new WordFinderUtil();
        searchResult = new SearchResult();
    }

    @Override
    public String beginSearch(String query, String siteFromQuery, Integer offset, Integer limit) throws IOException {
        System.out.println("!!! НАЧАЛО ПОИСКА performSearch !!!"); // удалить
        this.offset = offset;
        this.limitOutputResults = limit;
        List<IndexModel> matchingSearchIndexes = new ArrayList<>();
        Set<IndexModel> tempMatchingIndexes = new HashSet<>();

        /* 1. Разбиваем поисковый запрос на отдельные слова и формируем из этих слов список уникальных лемм,
        исключая междометия, союзы, предлоги и частицы + добавим их количество*/
        Map<String, Integer> uniqueLemmasFromQuery = lemmaFinderUtil.getLemmasMap(query);
        System.out.println("СПИСОК ЛЕММ ИЗ ПОИСКОВОГО ЗАПРОСА С КОЛИЧЕСТВОМ :" + uniqueLemmasFromQuery); // удалить

        /* 1.1. По леммам из запроса и сайту, находим все модели-лемм из репозитория*/
        List<LemmaModel> listLemmaModelsFromRepository = getListLemmaModelsByLemmaFromQuery(uniqueLemmasFromQuery,
                siteFromQuery);
        Map<String, Integer> listLemmas = getLemma(listLemmaModelsFromRepository);

        /* 1.2. По найденным моделям-леммам, находим все страницы, на которых она встречается.*/
        Map<String, List<PageModel>> listLemmasAndPages = getPagesByLemmaModel(listLemmaModelsFromRepository);

        /* 2. Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
        Поэкспериментируйте и определите этот процент самостоятельно.*/



        /* 3.Сортировать леммы в порядке увеличения частоты встречаемости
        (по возрастанию значения поля frequency) — от самых редких до самых частых./*/
        Map<String, Integer> sortedLemmasByFrequency = getSortedLemmasByFrequency(uniqueLemmasFromQuery);

        /*По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.
        Искать соответствия следующей леммы из этого списка страниц. 
        Повторять операцию по каждой следующей лемме. 
        Список страниц при этом на каждой итерации должен уменьшаться.*/
//        List<LemmaModel> listLemmaModelsFromRepository = getListPagesByLemma(sortedLemmasByFrequency, siteFromQuery);

            // если находим лемму
            // ищем по репозиторию индексов страницы, соответствующие лемме
            // сохраняем перечень моделей индексов в set
            if (!listLemmaModelsFromRepository.isEmpty()) {
                listLemmaModelsFromRepository.forEach(lemma -> {
                    tempMatchingIndexes.addAll(indexRepository.findByLemmaId(lemma));
                });
            }
//        }


        if (!tempMatchingIndexes.isEmpty()) {
            matchingSearchIndexes.addAll(tempMatchingIndexes);
        }

        /*Если страницы найдены, рассчитывать по каждой из них релевантность.*/
        Map<Integer, Double> relevancePage = calculateMaxPageRelevance(matchingSearchIndexes);
        /*Список объектов страниц с учетом полученных данных.*/
        List<PageData> pageDataList = setPageData(matchingSearchIndexes,
                sortedLemmasByFrequency, relevancePage);
        /*Сортировать страницы по убыванию релевантности (от большей к меньшей)*/
        pageDataList.sort((pageData1, pageData2) ->
                Double.compare(pageData2.getRelevance(), pageData1.getRelevance()));

        setSearchResult(pageDataList, matchingSearchIndexes.size());

        ObjectMapper objectMapper = new ObjectMapper();

        // потом удалить
        searchResult.getData().forEach(data ->
                System.out.println(data.getSnippet()));
        return objectMapper.writeValueAsString(searchResult);
    }

    //--------------------------------------------------------------------------------------------------------------
    /* По леммам из запроса, находим все модели лемм из репозитория с учетом модели сайта.*/
    private List<LemmaModel> getListLemmaModelsByLemmaFromQuery(Map<String, Integer> uniqueLemmasFromQuery,
                                                                String siteFromQuery) {
        List<LemmaModel> listLemmaModelsFromRepository = new ArrayList<>();
        for (String lemmaFromSortedListQuery : uniqueLemmasFromQuery.keySet()) {
            /* если сайт для поиска не задан, поиск должен происходить по всем проиндексированным сайтам,
             ищем леммы из всего репозитория*/
            if (siteFromQuery == null) {
                listLemmaModelsFromRepository = lemmaRepository.findAllByLemma(lemmaFromSortedListQuery);
            } else {
                SiteModel siteModel = siteRepository.findSiteModelByUrl(siteFromQuery);
                LemmaModel lemmaModelFromRepository = lemmaRepository.findByLemmaAndSiteId(lemmaFromSortedListQuery,
                        siteModel);
                listLemmaModelsFromRepository.add(lemmaModelFromRepository);
            }
        }
        System.out.println("НАШЛИ МОДЕЛИ ЛЕМЫ В РЕПОЗИТОРИИ " + listLemmaModelsFromRepository.size()); // удалить
        return listLemmaModelsFromRepository;
    }

    //--------------------------------------------------------------------------------------------------------------
    /* По моделям лемм, находим все страницы, на которых она встречается.*/
    private Map<String, List<PageModel>> getPagesByLemmaModel(List<LemmaModel> listLemmaModelsFromRepository) {
        Map<String, List<PageModel>> listLemmasAndPages = new LinkedHashMap<>();
        listLemmaModelsFromRepository.forEach(lemmaModel -> {
            List<PageModel> listPageModels = indexRepository.findByLemmaId(lemmaModel).stream()
                    .map(IndexModel::getPageId)
                    .collect(Collectors.toList());
            listLemmasAndPages.put(lemmaModel.getLemma(), listPageModels);
        });
        return listLemmasAndPages;
    }

    //--------------------------------------------------------------------------------------------------------------
    /* 2. Исключать из полученного списка леммы, которые встречаются на слишком большом количестве страниц.
        Поэкспериментируйте и определите этот процент самостоятельно.*/
    private Map<String, Integer> getLemma(List<LemmaModel> listLemmaModelsFromRepository) {
        Map<String, Integer> listLemmas = new LinkedHashMap<>();
        listLemmaModelsFromRepository.forEach(lemmaModel -> {
            List<PageModel> listPageModels = indexRepository.findByLemmaId(lemmaModel).stream()
                    .map(IndexModel::getPageId)
                    .toList();
            listLemmas.put(lemmaModel.getLemma(), listPageModels.size());
        });
        System.out.println(listLemmas);
        return listLemmas;
    }

    //--------------------------------------------------------------------------------------------------------------
    /* Сортировать леммы в порядке увеличения частоты встречаемости (по возрастанию значения поля frequency)
     — от самых редких до самых частых./*/
    private Map<String, Integer> getSortedLemmasByFrequency(Map<String, Integer> uniqueLemmas) {
        Map<String, Integer> sortedLemmasByFrequency = uniqueLemmas.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
        System.out.println("ОТСОРТИРОВАННЫЕ ЛЕММЫ ПО ВОЗРАСТАНИЮ ВСТРЕЧАЕМОСТИ ИЗ ЗАПРОСА ДЛЯ ПОИСКА "
                + sortedLemmasByFrequency); // удалить
        return sortedLemmasByFrequency;
    }

    //--------------------------------------------------------------------------------------------------------------
    /*По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.*/
    private List<LemmaModel> getListPagesByLemma(Map<String, Integer> sortedLemmasByFrequency,
                                                 String siteFromQuery) {
        List<LemmaModel> listLemmaModelsFromRepository = new ArrayList<>();
        for (String lemmaFromSortedListQuery : sortedLemmasByFrequency.keySet()) {
            /* если сайт для поиска не задан, поиск должен происходить по всем проиндексированным сайтам,
             ищем леммы из всего репозитория*/
            if (siteFromQuery == null) {
                listLemmaModelsFromRepository = lemmaRepository.findAllByLemma(lemmaFromSortedListQuery);
            } else {
                SiteModel siteModel = siteRepository.findSiteModelByUrl(siteFromQuery);
                LemmaModel lemmaModelFromRepository = lemmaRepository.findByLemmaAndSiteId(lemmaFromSortedListQuery,
                        siteModel);
                listLemmaModelsFromRepository.add(lemmaModelFromRepository);
            }
            System.out.println("НАШЛИ ЛЕММЫ В РЕПОЗИТОРИИ " + listLemmaModelsFromRepository.size()); // удалить
        }
        return listLemmaModelsFromRepository;
    }

    //--------------------------------------------------------------------------------------------------------------



    private void setSearchResult(List<PageData> pageDataList, int count) {
        searchResult.setData(pageDataList);
        searchResult.setCount(count);
        searchResult.setResult(true);
    }

    private List<PageData> setPageData(List<IndexModel> matchingSearchIndexList,
                                       Map<String, Integer> sortedLemmasByFrequency,
                                       Map<Integer, Double> relevance) throws IOException {
        List<PageData> pageDataResult = new ArrayList<>();
        double maxRelevance = Collections.max(relevance.values());
        int startOffset = Math.max(offset != null ? offset : 0, 0);
        // limit — количество результатов, которое необходимо вывести (параметр необязательный;
        // если не установлен, то значение по умолчанию равно 20).
        int endOffset = Math.min(startOffset + (limitOutputResults != null &&
                limitOutputResults > 0 ? limitOutputResults : 20), matchingSearchIndexList.size());
        Set<Integer> uniquePageId = new HashSet<>();
        for (int i = startOffset; i < endOffset; i++) {
            int newPageId = matchingSearchIndexList.get(i).getPageId().getId();
            if (uniquePageId.add(newPageId)) {
                String siteName = matchingSearchIndexList.get(i).getPageId().getSiteId().getName();
                String url = matchingSearchIndexList.get(i).getPageId().getPath();
                String site = matchingSearchIndexList.get(i).getPageId().getSiteId().getUrl();
                String fullContentPage = matchingSearchIndexList.get(i).getPageId().getContent();

                List<String> lemmas = new ArrayList<>(sortedLemmasByFrequency.keySet());
                System.out.println("+++++++++++++ ПРОВЕРКА LEMMAS " + lemmas);

                String title = wordFinderUtil.getTitleFromFullContentPage(fullContentPage);
                String snippet = wordFinderUtil.getSnippet(fullContentPage, lemmas);

                PageData pageData = new PageData();
                double absolutRelevance = relevance.getOrDefault(newPageId, 0.0);
                pageData.setSiteName(siteName);
                pageData.setUrl(url);
                pageData.setSite(site);
                pageData.setSnippet("<b>" + snippet + "</b>");
                pageData.setTitle(title);
                pageData.setRelevance(absolutRelevance / maxRelevance);
                pageDataResult.add(pageData);
            }
        }
        return pageDataResult;
    }

    private Map<Integer, Double> calculateMaxPageRelevance(List<IndexModel> matchingSearchIndexes) {
        Map<Integer, Double> pageRelevenceMap = new HashMap<>();
        for (IndexModel indexModel : matchingSearchIndexes) {
            double relevance = indexModel.getRank();
            int pageId = indexModel.getPageId().getId();
            if (pageRelevenceMap.containsKey(pageId)) {
                double currentPageRelevance = pageRelevenceMap.get(pageId);
                double newPageRelevance = currentPageRelevance + relevance;
                pageRelevenceMap.put(pageId, newPageRelevance);
            } else {
                pageRelevenceMap.put(pageId, relevance);
            }
        }
        return pageRelevenceMap;
    }
}


        // МУСОРКА НА ВСЯКИЙ СЛУЧАЙ

//    @Override
//    public String beginSearch(String query, String siteFromQuery, Integer offset, Integer limit) throws IOException {
//        System.out.println("!!! НАЧАЛО ПОИСКА performSearch !!!"); // удалить
//        this.offset = offset;
//        this.limitOutputResults = limit;
//        List<IndexModel> matchingSearchIndexes = new ArrayList<>();
//        Set<IndexModel> tempMatchingIndexes = new HashSet<>();
//        /*Разбиваем поисковый запрос на отдельные слова и формируем из этих слов список уникальных лемм,
//        исключая междометия, союзы, предлоги и частицы.
//        + добавим их количество*/
//        Map<String, Integer> uniqueLemmasFromQuery = lemmaFinderUtil.getLemmasMap(query);
//        System.out.println("СПИСОК ЛЕММ ИЗ ПОИСКОВОГО ЗАПРОСА С КОЛИЧЕСТВОМ :" + uniqueLemmasFromQuery); // удалить
//
//        /*Сортировать леммы в порядке увеличения частоты встречаемости
//        (по возрастанию значения поля frequency) — от самых редких до самых частых./*/
//        Map<String, Integer> sortedLemmasByFrequency = getSortedLemmasByFrequency(uniqueLemmasFromQuery);
//
////        Map<String, Integer> getSortedLemmasByFrequency = uniqueLemmasFromQuery.entrySet()
////                .stream()
////                .sorted(Map.Entry.comparingByValue())
////                .collect(LinkedHashMap::new, (map, entry) ->
////                        map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
////        System.out.println("ОТСОРТИРОВАННЫЕ ЛЕММЫ ПО ВОЗРАСТАНИЮ ИЗ ЗАПРОСА ДЛЯ ПОИСКА "
////                + getSortedLemmasByFrequency); // удалить
//
//
//        /*По первой, самой редкой лемме из списка, находить все страницы, на которых она встречается.
//        Искать соответствия следующей леммы из этого списка страниц.
//        Повторять операцию по каждой следующей лемме.
//        Список страниц при этом на каждой итерации должен уменьшаться.*/
//        List<LemmaModel> listLemmaModelsFromRepository = getListPagesByLemma(sortedLemmasByFrequency, siteFromQuery);
//
//        for (String lemmaFromSortedListQuery : sortedLemmasByFrequency.keySet()) {
//            List<LemmaModel> listLemmaModelsFromRepository = new ArrayList<>();
//            // если сайт для поиска не задан,
//            // поиск должен происходить по всем проиндексированным сайтам
//            // ищем леммы из всего репозитория
//            if (siteFromQuery == null) {
//                listLemmaModelsFromRepository = lemmaRepository.findAllByLemma(lemmaFromSortedListQuery);
//            } else {
//                SiteModel siteModel = siteRepository.findSiteModelByUrl(siteFromQuery);
//                LemmaModel lemmaModelFromRepository = lemmaRepository.findByLemmaAndSiteId(lemmaFromSortedListQuery, siteModel);
//                listLemmaModelsFromRepository.add(lemmaModelFromRepository);
//            }
//
//            System.out.println("++++ НАШЛИ ЛЕММЫ " + listLemmaModelsFromRepository.size()); // удалить
//
//            // если находим лемму
//            // ищем по репозиторию индексов страницы, соответствующие лемме
//            // сохраняем перечень моделей индексов в set
//            if (!listLemmaModelsFromRepository.isEmpty()) {
//                listLemmaModelsFromRepository.forEach(lemma -> {
//                    tempMatchingIndexes.addAll(indexRepository.findByLemmaId(lemma));
//                });
//            }
//        }
//
//
//        if (!tempMatchingIndexes.isEmpty()) {
//            matchingSearchIndexes.addAll(tempMatchingIndexes);
//        }
//
//        /*Если страницы найдены, рассчитывать по каждой из них релевантность.*/
//        Map<Integer, Double> relevancePage = calculateMaxPageRelevance(matchingSearchIndexes);
//        /*Список объектов страниц с учетом полученных данных.*/
//        List<PageData> pageDataList = setPageData(matchingSearchIndexes,
//                sortedLemmasByFrequency, relevancePage);
//        /*Сортировать страницы по убыванию релевантности (от большей к меньшей)*/
//        pageDataList.sort((pageData1, pageData2) ->
//                Double.compare(pageData2.getRelevance(), pageData1.getRelevance()));
//
//        setSearchResult(pageDataList, matchingSearchIndexes.size());
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // потом удалить
//        searchResult.getData().forEach(data ->
//                System.out.println(data.getSnippet()));
//        return objectMapper.writeValueAsString(searchResult);
//    }