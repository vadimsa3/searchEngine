package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "page_index")
@Data
@NoArgsConstructor
@Table(name = "page_index", indexes = @Index(columnList = "path_page, site_id", name = "path_index", unique = true))

public class PageModel {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    @JoinColumn(name = "site_id")
    private SiteModel siteId;

    @Column(columnDefinition = "TEXT", name = "path_page", nullable = false)
    private String path;

    @Column(name = "code_response", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", name = "page_content", nullable = false)
    private String content;
}