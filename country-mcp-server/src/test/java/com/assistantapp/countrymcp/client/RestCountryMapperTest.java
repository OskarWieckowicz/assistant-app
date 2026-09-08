package com.assistantapp.countrymcp.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.assistantapp.countrymcp.client.dto.RestCountry;
import com.assistantapp.countrymcp.country.CountryDetails;

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
                83_445_000L,
                List.of(new RestCountry.Currency("EUR", "Euro", "€")),
                List.of(new RestCountry.Language("German")),
                new RestCountry.Area(357114.5),
                new RestCountry.Cars("right"),
                List.of("49"),
                new RestCountry.Memberships(true, true));

        var result = mapper.map(source);

        assertThat(result.name()).isEqualTo("Germany");
        assertThat(result.officialName()).isEqualTo("Federal Republic of Germany");
        assertThat(result.capital()).isEqualTo("Berlin");
        assertThat(result.region()).isEqualTo("Europe");
        assertThat(result.subregion()).isEqualTo("Western Europe");
        assertThat(result.population()).isEqualTo(83_445_000L);
        assertThat(result.currencies()).containsExactly(
                new CountryDetails.Currency("EUR", "Euro", "€"));
        assertThat(result.languages()).containsExactly("German");
        assertThat(result.areaSquareKilometers()).isEqualTo(357114.5);
        assertThat(result.drivingSide()).isEqualTo("right");
        assertThat(result.callingCodes()).containsExactly("49");
        assertThat(result.euMember()).isTrue();
        assertThat(result.schengenMember()).isTrue();
    }

    @Test
    void fallsBackToFirstCapitalWhenNoneIsPrimary() {
        RestCountry source = new RestCountry(
                new RestCountry.Names("Country", "Country"),
                List.of(new RestCountry.Capital("First capital", false)),
                "Region",
                "Subregion",
                1L, null, null, null, null, null, null);

        assertThat(mapper.map(source).capital()).isEqualTo("First capital");
    }

    @Test
    void preservesUnavailableFieldsWithoutInventingValues() {
        var result = mapper.map(new RestCountry(new RestCountry.Names("Country", "Country"),
                null, null, null, null, null, null, null, null, null, null));

        assertThat(result.currencies()).isNull();
        assertThat(result.languages()).isNull();
        assertThat(result.areaSquareKilometers()).isNull();
        assertThat(result.drivingSide()).isNull();
        assertThat(result.callingCodes()).isNull();
        assertThat(result.euMember()).isNull();
        assertThat(result.schengenMember()).isNull();
    }

    @Test
    void preservesMultipleValuesAndDistinguishesFalseFromUnknownMembership() {
        var result = mapper.map(new RestCountry(new RestCountry.Names("Country", "Country"),
                null, null, null, null,
                List.of(new RestCountry.Currency("AAA", "First", "$"),
                        new RestCountry.Currency("BBB", "Second", null)),
                List.of(new RestCountry.Language("English"), new RestCountry.Language("French")),
                new RestCountry.Area(null), new RestCountry.Cars(null), List.of("1", "2"),
                new RestCountry.Memberships(false, null)));

        assertThat(result.currencies()).extracting("code").containsExactly("AAA", "BBB");
        assertThat(result.languages()).containsExactly("English", "French");
        assertThat(result.callingCodes()).containsExactly("1", "2");
        assertThat(result.areaSquareKilometers()).isNull();
        assertThat(result.drivingSide()).isNull();
        assertThat(result.euMember()).isFalse();
        assertThat(result.schengenMember()).isNull();
    }
}
