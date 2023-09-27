package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity(name = "lemma_index")
@Data
@NoArgsConstructor

// ???
@Table(name = "lemma_index", indexes = @Index(columnList = "lemma", name = "lemma_index"))

public class LemmaModel {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(cascade = {CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    // ???
//    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE)
//    private List<IndexModel> indexes;
}
