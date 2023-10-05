package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity(name = "search_index")
@Data
@NoArgsConstructor

public class IndexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "page_id", nullable = false)
    private PageModel pageId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaModel lemmaId;

    @Column(name = "amount", columnDefinition = "FLOAT", nullable = false)
    private float rank;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private SiteModel siteModel;
}