package com.assistantapp.countrymcp.client;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.assistantapp.countrymcp.client.dto.RestCountry;
import com.assistantapp.countrymcp.client.dto.RestCountriesResponse;
import com.assistantapp.countrymcp.country.CountryDetails;
import com.assistantapp.countrymcp.country.CountryGateway;

@Component
public class RestCountriesGatewayAdapter implements CountryGateway {

    private final RestCountriesClient client;
    private final RestCountryMapper mapper;

    public RestCountriesGatewayAdapter(
            RestCountriesClient client,
            RestCountryMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Optional<CountryDetails> findByName(String countryName) {
        String normalizedName = normalize(countryName);

        return client.findByName(normalizedName)
                .flatMap(this::firstCountry)
                .map(mapper::map);
    }

    private Optional<RestCountry> firstCountry(RestCountriesResponse response) {
        if (response.data() == null || response.data().objects() == null) {
            return Optional.empty();
        }

        return response.data().objects().stream().findFirst();
    }

    private String normalize(String countryName) {
        if (!StringUtils.hasText(countryName)) {
            throw new IllegalArgumentException("Country name must not be blank");
        }

        return countryName.trim();
    }
}
