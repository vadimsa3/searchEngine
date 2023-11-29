package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageData {
    private String site;
    private String siteName;
    private String url;
    private String title;
//    private String snippet;
    private List<String> snippet;
    private Double relevance;
}
