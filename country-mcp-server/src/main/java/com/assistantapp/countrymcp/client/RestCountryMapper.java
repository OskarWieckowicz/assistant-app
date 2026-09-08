package com.assistantapp.countrymcp.client;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.assistantapp.countrymcp.client.dto.RestCountry;
import com.assistantapp.countrymcp.country.CountryDetails;

@Component
public class RestCountryMapper {

    public CountryDetails map(RestCountry source) {
        if (source == null || source.names() == null) {
            throw new RestCountriesClientException(
                    "REST Countries returned a country without a name");
        }

        return new CountryDetails(
                source.names().common(),
                source.names().official(),
                selectCapital(source.capitals()),
                source.region(),
                source.subregion(),
                source.population(),
                source.currencies() == null ? null : source.currencies().stream()
                        .filter(Objects::nonNull)
                        .map(currency -> new CountryDetails.Currency(
                                currency.code(), currency.name(), currency.symbol()))
                        .toList(),
                source.languages() == null ? null : source.languages().stream()
                        .filter(Objects::nonNull)
                        .map(RestCountry.Language::name)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                source.area() == null ? null : source.area().kilometers(),
                source.cars() == null ? null : source.cars().drivingSide(),
                source.callingCodes(),
                source.memberships() == null ? null : source.memberships().eu(),
                source.memberships() == null ? null : source.memberships().schengen());
    }

    private String selectCapital(List<RestCountry.Capital> capitals) {
        if (capitals == null || capitals.isEmpty()) {
            return null;
        }

        return capitals.stream()
                .filter(Objects::nonNull)
                .filter(capital -> Boolean.TRUE.equals(capital.primary()))
                .map(RestCountry.Capital::name)
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> firstCapital(capitals))
                .orElse(null);
    }

    private Optional<String> firstCapital(List<RestCountry.Capital> capitals) {
        return capitals.stream()
                .filter(Objects::nonNull)
                .map(RestCountry.Capital::name)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
