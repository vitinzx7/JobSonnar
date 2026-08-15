package jobsonnar.config;

import java.time.Duration;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
            requestFactory.setReadTimeout(READ_TIMEOUT);
            builder.requestFactory(requestFactory);
        };
    }
}
