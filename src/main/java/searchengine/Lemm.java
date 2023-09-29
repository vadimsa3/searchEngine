package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.LemmaFinderUtil;
import searchengine.utilities.LemmaModelUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Lemm {

    public static void main(String[] args) throws IOException {
//        LuceneMorphology luceneMorph =
//                new RussianLuceneMorphology();
//        List<String> wordBaseForms =
//                luceneMorph.getNormalForms("леса");
//        wordBaseForms.forEach(System.out::println);


        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        LemmaFinderUtil lemmaFinderUtil = new LemmaFinderUtil(luceneMorph);

        String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
                "что леопард постоянно обитает в некоторых районах Северного\n" +
                "Кавказа.";
//        System.out.println(new HashMap<>(lemmaFinderUtil.getLemmasMap(text)));
//        System.out.println(lemmaFinderUtil.getLemmasMap(text).size());
//        System.out.println(new HashSet<>(lemmaFinderUtil.getUniqLemmasSet(text)));


    }

}


