package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "page_index")
@Getter
@Setter
@NoArgsConstructor

public class PageModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    @JoinColumn(name = "site_id")
    private SiteModel siteId;

    @Column(columnDefinition = "TEXT", name = "path_page")
    private String path;

    @Column(name = "code_response")
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", name = "page_content")
    private String content;
}