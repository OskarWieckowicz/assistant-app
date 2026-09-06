package com.assistantapp.countrymcp.tool;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.assistantapp.countrymcp.country.CountryDetails;
import com.assistantapp.countrymcp.country.CountryGateway;
import com.assistantapp.countrymcp.country.CountryNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryToolsTest {

    @Test
    void returnsCountryProvidedByGateway() {
        CountryDetails germany = new CountryDetails(
                "Germany",
                "Federal Republic of Germany",
                "Berlin",
                "Europe",
                "Western Europe",
                83_445_000L);
        StubCountryGateway gateway = new StubCountryGateway(Optional.of(germany));
        CountryTools tools = new CountryTools(gateway);

        CountryDetails result = tools.getCountry("Germany");

        assertThat(result).isEqualTo(germany);
        assertThat(gateway.requestedCountryName()).isEqualTo("Germany");
    }

    @Test
    void throwsCountryNotFoundExceptionWhenGatewayReturnsEmpty() {
        StubCountryGateway gateway = new StubCountryGateway(Optional.empty());
        CountryTools tools = new CountryTools(gateway);

        assertThatThrownBy(() -> tools.getCountry("Unknown"))
                .isInstanceOf(CountryNotFoundException.class)
                .hasMessage("Country not found: Unknown");
        assertThat(gateway.requestedCountryName()).isEqualTo("Unknown");
    }

    private static final class StubCountryGateway implements CountryGateway {

        private final Optional<CountryDetails> result;
        private String requestedCountryName;

        private StubCountryGateway(Optional<CountryDetails> result) {
            this.result = result;
        }

        @Override
        public Optional<CountryDetails> findByName(String countryName) {
            requestedCountryName = countryName;
            return result;
        }

        private String requestedCountryName() {
            return requestedCountryName;
        }
    }
}
