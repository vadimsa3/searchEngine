package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.*;

@Entity(name = "site_index")
@Data
@NoArgsConstructor

public class SiteModel {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", name = "status_index", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusSiteIndex statusSiteIndex;

    @Column(columnDefinition = "DATETIME", name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", name = "site_name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "siteId", cascade = CascadeType.REMOVE) // связь с индексаим
    private List<PageModel> pageModel;
//
//    @OneToMany(mappedBy = "siteId", cascade = CascadeType.REMOVE)
//    private List<LemmaModel> lemmaModel;
//
//    @OneToMany(mappedBy = "siteId", cascade = CascadeType.REMOVE)
//    private List<IndexModel> indexModel;
}