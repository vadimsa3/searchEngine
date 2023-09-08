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
import javax.persistence.Table;

@Entity
@Table(name = "lemma_index")

public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;

    @ManyToOne(cascade = {CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    public LemmaModel() {
    }

    public int getId() {
        return this.id;
    }

    public SiteModel getSiteId() {
        return this.siteId;
    }

    public String getLemma() {
        return this.lemma;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setSiteId(final SiteModel siteId) {
        this.siteId = siteId;
    }

    public void setLemma(final String lemma) {
        this.lemma = lemma;
    }

    public void setFrequency(final int frequency) {
        this.frequency = frequency;
    }
}
