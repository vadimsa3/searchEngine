package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "search_index")
@Getter
@Setter
@NoArgsConstructor

public class IndexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = PageModel.class)
    @JoinColumn(name = "page_id", nullable = false)
    private PageModel pageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = LemmaModel.class)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaModel lemmaId;

    @Column(name = "amount", columnDefinition = "FLOAT", nullable = false)
    private float rank;
}