package com.assistantapp.countrymcp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestCountriesClientTest {

    private static final String BASE_URL = "https://countries.example.test";

    private MockRestServiceServer server;
    private RestCountriesClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestCountriesClient(builder.build());
    }

    @Test
    void returnsDeserializedResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/names.common/Germany"))
                .andExpect(request -> assertThat(UriComponentsBuilder.fromUri(request.getURI()).build()
                        .getQueryParams().getFirst("response_fields").split(","))
                        .containsExactlyInAnyOrder("names.common", "names.official", "capitals", "region",
                                "subregion", "population", "currencies", "languages", "area.kilometers",
                                "cars.driving_side", "calling_codes", "memberships.eu", "memberships.schengen"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "objects": [{
                              "names": {
                                "common": "Germany",
                                "official": "Federal Republic of Germany"
                              },
                              "capitals": [{ "name": "Berlin", "primary": true }],
                              "region": "Europe",
                              "subregion": "Western Europe",
                              "population": 83445000,
                              "currencies": [{"code": "EUR", "name": "Euro", "symbol": "€"}],
                              "languages": [{"name": "German", "iso639_1": "de", "native_name": "Deutsch"}],
                              "area": {"kilometers": 357114.5},
                              "cars": {"driving_side": "right"},
                              "calling_codes": ["49"],
                              "memberships": {"eu": true, "schengen": false}
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.findByName("Germany");

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().data().objects().getFirst().names().common())
                .isEqualTo("Germany");
        var country = new RestCountryMapper().map(response.orElseThrow().data().objects().getFirst());
        assertThat(country.currencies()).extracting("code").containsExactly("EUR");
        assertThat(country.languages()).containsExactly("German");
        assertThat(country.areaSquareKilometers()).isEqualTo(357114.5);
        assertThat(country.drivingSide()).isEqualTo("right");
        assertThat(country.callingCodes()).containsExactly("49");
        assertThat(country.euMember()).isTrue();
        assertThat(country.schengenMember()).isFalse();
        server.verify();
    }

    @Test
    void returnsEmptyForNotFoundResponse() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/names.common/Unknown"))
                .andRespond(withStatus(NOT_FOUND));

        assertThat(client.findByName("Unknown")).isEmpty();
        server.verify();
    }
}
