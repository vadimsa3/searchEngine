package searchengine.utilities;

public class WordFinderUtil {


//    public String performSearch(String query, String site, Integer offset, Integer limit) throws IOException {
//        this.offset = offset;
//        this.limit = limit;
//        List<SearchIndex> matchingSearchIndexes = new ArrayList<>();
//        List<SearchIndex> tempMatchingIndexes = new ArrayList<>();
//        Map<String, Integer> lemmas = lemmatizationUtils.getLemmaMap(query);
//        Map<String, Integer> sortedLemmasByFrequency = lemmas.entrySet()
//                .stream()
//                .sorted(Map.Entry.comparingByValue())
//                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
//        for (String word : sortedLemmasByFrequency.keySet()) {
//            Optional<Lemma> lemmaRep;
//            if (site == null) {
//                lemmaRep = lemmaRepositories.findByLemma(word);
//            } else {
//                SiteTable siteTable = siteRepositories.findByUrl(site);
//                lemmaRep = lemmaRepositories.findByLemmaAndSiteId(word, siteTable);
//            }
//            if (lemmaRep.isPresent()) {
//                Lemma lemma = lemmaRep.get();
//                tempMatchingIndexes.addAll(searchIndexRepositories.findByLemmaId(lemma));
//            }
//        }
//        if (!tempMatchingIndexes.isEmpty()) {
//            matchingSearchIndexes.addAll(tempMatchingIndexes);
//        }


        private String checkEnterWordLanguage (String word){
            String russianAlphabet = "[а-яА-Я]+";
            String englishAlphabet = "[a-zA-z]+";
            if (word.matches(russianAlphabet)) {
                return "Russian";
            } else if (word.matches(englishAlphabet)) {
                return "Enter a word in Russian";
            } else {
                return "";
            }
        }
    }
