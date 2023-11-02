package searchengine.utilities;

public class WordFinderUtil {


    public class FindWordInText {

        private final LemmatizationUtils lemmatizationUtils;
        @SneakyThrows
        public FindWordInText() {
            lemmatizationUtils = new LemmatizationUtils();

        }

        public String getMatchingSnippet(String fullText, List<String> lemmas) {
            String text = clearTagOnHtml(fullText);
            System.out.println(lemmas);
            String newText = text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
            String[] words = newText.split("\\s");

            List<Integer> indexInText = new ArrayList<>();
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                for (String lemma : lemmas) {
                    if (isLemmaInText(lemma, word)) {
                        indexInText.add(text.toLowerCase().indexOf(word));
                    }
                }
            }

            Collections.sort(indexInText);

            int bestStartIndex = 0;
            int bestEndIndex = 0;
            int maxLemmas = 0;

            for (int i = 0; i < indexInText.size(); i++) {
                int startIndex = indexInText.get(i);
                int endIndex = startIndex + 150;
                int nextSpaceIndex = text.indexOf(" ", endIndex);
                if (nextSpaceIndex != -1) {
                    endIndex = nextSpaceIndex;
                }

                int currentLemmas = 0;
                for (String lemma : lemmas) {
                    if (text.toLowerCase().substring(startIndex, endIndex).contains(lemma)) {
                        currentLemmas++;
                    }
                }
                if (currentLemmas > maxLemmas) {
                    maxLemmas = currentLemmas;
                    bestStartIndex = startIndex;
                    bestEndIndex = endIndex;
                }
            }

            return text.substring(bestStartIndex, bestEndIndex);
        }
        public String getTitle(String html){
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            return title;
        }
        private String clearTagOnHtml(String html){
            Document doc = Jsoup.parse(html);
            String text = doc.text();
            return text;
        }
        public boolean isLemmaInText(String lemma, String word){
            if(word.isBlank()){
                return false;
            }
            List<String> ruWordBaseForms = lemmatizationUtils.ruLuceneMorph.getMorphInfo(word);
            if(lemmatizationUtils.anyWordBaseBelongToParticle(ruWordBaseForms)){
                return false;
            }
            List<String> normalForms = lemmatizationUtils.ruLuceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) {
                return false;
            }
            return normalForms.get(0).equals(lemma);
        }
    }


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
