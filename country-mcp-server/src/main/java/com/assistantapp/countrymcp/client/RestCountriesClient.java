package com.assistantapp.countrymcp.client;

import java.util.Optional;

import com.assistantapp.countrymcp.client.dto.RestCountriesResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestCountriesClient {

        private static final String RESPONSE_FIELDS = "names.common,names.official,capitals,region,subregion,population,"
                        + "currencies,languages,area.kilometers,cars.driving_side,calling_codes,memberships.eu,memberships.schengen";

        private final RestClient restClient;

        public RestCountriesClient(
                        @Qualifier("restCountriesRestClient") RestClient restClient) {
                this.restClient = restClient;
        }

        public Optional<RestCountriesResponse> findByName(String countryName) {
                try {
                        RestCountriesResponse response = restClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/names.common/{countryName}")
                                                        .queryParam("response_fields", RESPONSE_FIELDS)
                                                        .build(countryName))
                                        .retrieve()
                                        .body(RestCountriesResponse.class);

                        return Optional.ofNullable(response);
                } catch (HttpClientErrorException.NotFound exception) {
                        return Optional.empty();
                } catch (RestClientException exception) {
                        throw new RestCountriesClientException(
                                        "REST Countries request failed for country: " + countryName,
                                        exception);
                }
        }
}
