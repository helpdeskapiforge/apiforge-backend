package com.apiplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * A separate {@link RestClient} from {@link RestClientConfig}'s proxy client on purpose:
 * LLM completions routinely take 10-40s (especially local Ollama models on CPU), which
 * would starve/mis-time a client tuned for proxying arbitrary short-lived user requests.
 * Connect timeout stays short (a provider that can't even open a socket is not "slow",
 * it's down) while read timeout is driven by {@code ai.request-timeout-seconds}.
 */
@Configuration
public class AIClientConfig {

    @Bean
    public RestClient aiRestClient(AIProperties aiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(aiProperties.getRequestTimeoutSeconds()).toMillis());

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
