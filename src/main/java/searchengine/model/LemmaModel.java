package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity(name = "lemma")
@Data
@NoArgsConstructor
@Table(indexes = @Index(columnList = "lemma", name = "lemma_index"))

public class LemmaModel {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

//    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SiteModel.class, cascade = CascadeType.REMOVE, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private SiteModel siteModel;

    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.REMOVE)
    private Set<IndexModel> indexModel;
}
