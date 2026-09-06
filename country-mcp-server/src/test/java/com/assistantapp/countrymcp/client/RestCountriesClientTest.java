package com.assistantapp.countrymcp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
                              "population": 83445000
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.findByName("Germany");

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().data().objects().getFirst().names().common())
                .isEqualTo("Germany");
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
