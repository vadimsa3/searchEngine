package searchengine.dto.statistics;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
