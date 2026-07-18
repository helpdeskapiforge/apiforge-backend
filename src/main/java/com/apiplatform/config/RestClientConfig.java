package com.apiplatform.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Previously {@code ProxyController} instantiated {@code new RestTemplate()} on
     * every single request with no timeout configured at all — a slow or
     * non-responding upstream host would tie up a server thread indefinitely, which is
     * a trivial denial-of-service vector when combined with a user-controlled target URL.
     */
    @Bean
    public RestTemplate proxyRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(15))
                .build();
    }
}
