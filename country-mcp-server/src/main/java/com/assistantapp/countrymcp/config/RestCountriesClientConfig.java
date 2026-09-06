package com.assistantapp.countrymcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
class RestCountriesClientConfig {

    @Bean
    RestClient restCountriesRestClient(
            RestClient.Builder builder,
            RestCountriesProperties properties) {

        RestClient.Builder configuredBuilder = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(properties.apiKey())) {
            configuredBuilder.defaultHeaders(headers -> headers.setBearerAuth(properties.apiKey()));
        }

        return configuredBuilder.build();
    }
}
