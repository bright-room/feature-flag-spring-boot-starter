package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "management.endpoints.web.exposure.include=feature-flags",
      "feature-flags.feature-names.new-feature=true",
      "feature-flags.feature-names.disabled-feature=false"
    })
class FeatureFlagEndpointIntegrationTest {

  RestClient restClient;

  @Test
  void shouldReturnOk_whenGetEndpointCalled() {
    var response =
        restClient.get().uri("/actuator/feature-flags").retrieve().toEntity(String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturnAllFeatureFlags_whenGetEndpointCalled() {
    var response =
        restClient
            .get()
            .uri("/actuator/feature-flags")
            .retrieve()
            .toEntity(FeatureFlagEndpointResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().features())
        .containsExactlyInAnyOrderEntriesOf(Map.of("new-feature", true, "disabled-feature", false));
  }

  FeatureFlagEndpointIntegrationTest(@LocalManagementPort int port) {
    this.restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }
}
