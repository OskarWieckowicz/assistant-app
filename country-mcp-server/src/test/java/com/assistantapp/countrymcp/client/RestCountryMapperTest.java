package com.assistantapp.countrymcp.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.assistantapp.countrymcp.client.dto.RestCountry;

import static org.assertj.core.api.Assertions.assertThat;

class RestCountryMapperTest {

    private final RestCountryMapper mapper = new RestCountryMapper();

    @Test
    void mapsCountryAndSelectsPrimaryCapital() {
        RestCountry source = new RestCountry(
                new RestCountry.Names("Germany", "Federal Republic of Germany"),
                List.of(
                        new RestCountry.Capital("Bonn", false),
                        new RestCountry.Capital("Berlin", true)),
                "Europe",
                "Western Europe",
                83_445_000L);

        var result = mapper.map(source);

        assertThat(result.name()).isEqualTo("Germany");
        assertThat(result.officialName()).isEqualTo("Federal Republic of Germany");
        assertThat(result.capital()).isEqualTo("Berlin");
        assertThat(result.region()).isEqualTo("Europe");
        assertThat(result.subregion()).isEqualTo("Western Europe");
        assertThat(result.population()).isEqualTo(83_445_000L);
    }

    @Test
    void fallsBackToFirstCapitalWhenNoneIsPrimary() {
        RestCountry source = new RestCountry(
                new RestCountry.Names("Country", "Country"),
                List.of(new RestCountry.Capital("First capital", false)),
                "Region",
                "Subregion",
                1L);

        assertThat(mapper.map(source).capital()).isEqualTo("First capital");
    }
}
