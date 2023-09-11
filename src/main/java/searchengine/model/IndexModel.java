package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "search_index")
@Getter
@Setter

public class IndexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id", nullable = false)
    private PageModel pageId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaModel lemmaId;

    @Column(name = "amount", columnDefinition = "FLOAT", nullable = false)
    private float rank;
}