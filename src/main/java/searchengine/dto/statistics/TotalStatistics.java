package searchengine.dto.statistics;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TotalStatistics {
    private int sites;
    private long pages;
    private long lemmas;
    private boolean indexing;
}
