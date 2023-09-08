package searchengine.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "site_index")

public class SiteModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", name = "status_index")
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

    public int getId() {
        return this.id;
    }

    public StatusSiteIndex getStatusSiteIndex() {
        return this.statusSiteIndex;
    }

    public LocalDateTime getStatusTime() {
        return this.statusTime;
    }

    public String getLastError() {
        return this.lastError;
    }

    public String getUrl() {
        return this.url;
    }

    public String getName() {
        return this.name;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setStatusSiteIndex(final StatusSiteIndex statusSiteIndex) {
        this.statusSiteIndex = statusSiteIndex;
    }

    public void setStatusTime(final LocalDateTime statusTime) {
        this.statusTime = statusTime;
    }

    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public SiteModel(final int id, final StatusSiteIndex statusSiteIndex, final LocalDateTime statusTime, final String lastError, final String url, final String name) {
        this.id = id;
        this.statusSiteIndex = statusSiteIndex;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public SiteModel() {
    }
}