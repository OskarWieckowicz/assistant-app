package com.assistantapp.countrymcp.client;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.assistantapp.countrymcp.client.dto.RestCountry;
import com.assistantapp.countrymcp.client.dto.RestCountriesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RestCountriesGatewayAdapterTest {

    private final StubRestCountriesClient client = new StubRestCountriesClient();
    private final RestCountryMapper mapper = new RestCountryMapper();
    private final RestCountriesGatewayAdapter adapter =
            new RestCountriesGatewayAdapter(client, mapper);

    @Test
    void returnsMappedFirstCountry() {
        RestCountry germany = new RestCountry(
                new RestCountry.Names("Germany", "Federal Republic of Germany"),
                List.of(new RestCountry.Capital("Berlin", true)),
                "Europe",
                "Western Europe",
                83_445_000L);
        RestCountriesResponse response = new RestCountriesResponse(
                new RestCountriesResponse.Data(List.of(germany)));
        client.respondWith(response);

        var result = adapter.findByName(" Germany ");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().capital()).isEqualTo("Berlin");
        assertThat(client.requestedCountryName()).isEqualTo("Germany");
    }

    @Test
    void returnsEmptyWhenProviderReturnsNoCountries() {
        RestCountriesResponse response = new RestCountriesResponse(
                new RestCountriesResponse.Data(List.of()));
        client.respondWith(response);

        assertThat(adapter.findByName("Unknown")).isEmpty();
    }

    @Test
    void rejectsBlankNameBeforeCallingClient() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.findByName("  "))
                .withMessage("Country name must not be blank");
        assertThat(client.requestedCountryName()).isNull();
    }

    private static final class StubRestCountriesClient extends RestCountriesClient {

        private Optional<RestCountriesResponse> response = Optional.empty();
        private String requestedCountryName;

        private StubRestCountriesClient() {
            super(RestClient.create());
        }

        @Override
        public Optional<RestCountriesResponse> findByName(String countryName) {
            requestedCountryName = countryName;
            return response;
        }

        private void respondWith(RestCountriesResponse response) {
            this.response = Optional.of(response);
        }

        private String requestedCountryName() {
            return requestedCountryName;
        }
    }
}
