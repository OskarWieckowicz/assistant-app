package com.assistantapp.countrymcp.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rest-countries")
public record RestCountriesProperties(URI baseUrl, String apiKey) {
}
