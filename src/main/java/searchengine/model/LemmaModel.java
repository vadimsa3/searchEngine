package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity(name = "lemma")
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = @Index(columnList = "lemma", name = "lemma_index"))

public class LemmaModel {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

//    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SiteModel.class, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.REMOVE)
    private Set<IndexModel> indexModel;
}
