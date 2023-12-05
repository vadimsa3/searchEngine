package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter

@Component
@ConfigurationProperties(prefix = "connection-settings")

public class Connector {
    private String userAgent;
    private String referrer;
    private int timeout;
}
