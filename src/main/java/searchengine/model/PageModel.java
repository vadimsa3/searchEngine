package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.List;

@Entity(name = "page")
@Getter
@Setter
@NoArgsConstructor
@Table(indexes = @Index(columnList = "path_page, site_id", name = "path_index", unique = true))

public class PageModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SiteModel.class, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(columnDefinition = "TEXT", name = "path_page", nullable = false)
    private String path;

    @Column(name = "code_response", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", name = "page_content", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.REMOVE)
    private List<IndexModel> indexModel;
}