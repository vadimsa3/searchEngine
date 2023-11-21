package searchengine.config;

import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ToString
@EqualsAndHashCode

@Component
public class Site {
    private String url;
    private String name;
}
