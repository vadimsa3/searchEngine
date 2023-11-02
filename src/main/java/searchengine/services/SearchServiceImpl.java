package searchengine.services;

import org.springframework.stereotype.Service;


        /*Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).
        Чтобы выводить результаты порционно, также можно задать параметры offset (сдвиг от начала списка результатов)
        и limit (количество результатов, которое необходимо вывести).
        В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit,
        и массив data с результатами поиска.
        Каждый результат — это объект, содержащий свойства результата поиска (см. ниже структуру и описание каждого свойства).
        Если поисковый запрос не задан или ещё нет готового индекса
        (сайт, по которому ищем, или все сайты сразу не проиндексированы), метод должен вернуть соответствующую ошибку
        (см. ниже пример). Тексты ошибок должны быть понятными и отражать суть ошибок.*/

        /*Метод должен выполнять следующий алгоритм:
        Разбивать поисковый запрос на отдельные слова и формировать
        из этих слов список уникальных лемм, исключая междометия,
        союзы, предлоги и частицы. Используйте для этого код, который
        вы уже писали в предыдущем этапе.
        ● Исключать из полученного списка леммы, которые встречаются на
        слишком большом количестве страниц. Поэкспериментируйте и
        определите этот процент самостоятельно.
        ● Сортировать леммы в порядке увеличения частоты встречаемости
        (по возрастанию значения поля frequency) — от самых редких до
        самых частых.
        ● По первой, самой редкой лемме из списка, находить все страницы,
        на которых она встречается. Далее искать соответствия
        следующей леммы из этого списка страниц, а затем повторять
        операцию по каждой следующей лемме. Список страниц при этом
        на каждой итерации должен уменьшаться.
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

    private final SearchResult searchResult;
    @Autowired
    SearchIndexRepositories searchIndexRepositories;
    @Autowired
    LemmaRepositories lemmaRepositories;
    @Autowired
    SiteRepositories siteRepositories;
    private Integer offset;
    private Integer limit;
    private final LemmatizationUtils lemmatizationUtils;
    private final FindWordInText findWordInText;


    public SearchService() throws IOException {
        searchResult = new SearchResult();
        lemmatizationUtils = new LemmatizationUtils();
        findWordInText = new FindWordInText();
    }

    @Override
    public String performSearch(String query, String site, Integer offset, Integer limit) throws IOException {
        this.offset = offset;
        this.limit = limit;
        List<SearchIndex> matchingSearchIndexes = new ArrayList<>();
        Set<SearchIndex> tempMatchingIndexes = new HashSet<>();
        Map<String, Integer> lemmas = lemmatizationUtils.getLemmaMap(query);
        Map<String, Integer> sortedLemmasByFrequency = lemmas.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
        for (String word : sortedLemmasByFrequency.keySet()) {
            Optional<Lemma> lemmaRep;
            if (site == null) {
                lemmaRep = lemmaRepositories.findByLemma(word);
            } else {
                SiteTable siteTable = siteRepositories.findByUrl(site);
                lemmaRep = lemmaRepositories.findByLemmaAndSiteId(word, siteTable);
            }
            if (lemmaRep.isPresent()) {
                Lemma lemma = lemmaRep.get();
                tempMatchingIndexes.addAll(searchIndexRepositories.findByLemmaId(lemma));
            }
        }
        if (!tempMatchingIndexes.isEmpty()) {
            matchingSearchIndexes.addAll(tempMatchingIndexes);
        }

        Map<Integer, Double> relevance = calculateMaxRelevance(matchingSearchIndexes);
        List<PageData> pdList = setPageData(matchingSearchIndexes,
                sortedLemmasByFrequency, relevance);
        Collections.sort(pdList, (pd1, pd2) -> Double.compare(pd2.getRelevance(), pd1.getRelevance()));
        setSearchResult(pdList, matchingSearchIndexes.size());
        ObjectMapper objectMapper = new ObjectMapper();
        searchResult.getData().forEach(d -> System.out.println(d.getSnippet()));
        String jsonResult = objectMapper.writeValueAsString(searchResult);
        return jsonResult;
    }


    private void setSearchResult(List<PageData> pdList, int count) {
        searchResult.setData(pdList);
        searchResult.setCount(count);
        searchResult.setResult(true);
    }

    private List<PageData> setPageData(List<SearchIndex> matchingSearchIndexList,
                                       Map<String, Integer> sortedLemmasByFrequency,
                                       Map<Integer, Double> relevance) {
        List<PageData> pageDataResult = new ArrayList<>();
        double maxRelevance = Collections.max(relevance.values());
        int startOffset = Math.max(offset != null ? offset : 0, 0);
        int endOffset = Math.min(startOffset + (limit != null && limit > 0 ? limit : 20), matchingSearchIndexList.size());
        Set<Integer> uniqPageId = new HashSet<>();
        for (int i = startOffset; i < endOffset; i++) {
            int newPageId = matchingSearchIndexList.get(i).getPageId().getId();
            if (uniqPageId.add(newPageId)) {
                String siteName = matchingSearchIndexList.get(i).getPageId().getSiteId().getName();
                String url = matchingSearchIndexList.get(i).getPageId().getPath();
                String site = matchingSearchIndexList.get(i).getPageId().getSiteId().getUrl();
                String fullText = matchingSearchIndexList.get(i).getPageId().getContent();
                List<String> lemmas = new ArrayList<>(sortedLemmasByFrequency.keySet());
                String snippet = findWordInText.getMatchingSnippet(fullText, lemmas);
                String title = findWordInText.getTitle(fullText);
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


    private Map<Integer, Double> calculateMaxRelevance(List<SearchIndex> matchingSearchIndexes) {
        Map<Integer, Double> pageRelevenceMap = new HashMap<>();
        for (SearchIndex si : matchingSearchIndexes) {
            double relevance = si.getRank();
            int pageId = si.getPageId().getId();
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