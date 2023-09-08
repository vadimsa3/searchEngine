package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import javax.persistence.*;

@Entity(name = "site_index")
@Getter
@Setter
@NoArgsConstructor

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
}