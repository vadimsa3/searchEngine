package searchengine.model;

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

    public int getId() {
        return this.id;
    }

    public SiteModel getSiteId() {
        return this.siteId;
    }

    public String getPath() {
        return this.path;
    }

    public int getCode() {
        return this.code;
    }

    public String getContent() {
        return this.content;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setSiteId(final SiteModel siteId) {
        this.siteId = siteId;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setCode(final int code) {
        this.code = code;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public PageModel() {
    }

    public PageModel(final int id, final SiteModel siteId, final String path, final int code, final String content) {
        this.id = id;
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}