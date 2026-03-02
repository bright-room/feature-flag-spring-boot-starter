package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeatureFlagEndpointWriteIntegrationTest {

  RestClient restClient;

  @Test
  void shouldDisableFeatureFlag_whenWriteOperationCalledWithEnabledFalse() {
    var response =
        restClient
            .post()
            .uri("/actuator/feature-flags/new-feature")
            .contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"))
            .body("{\"enabled\":false}")
            .retrieve()
            .toEntity(Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var afterResponse =
        restClient
            .get()
            .uri("/actuator/feature-flags")
            .retrieve()
            .toEntity(FeatureFlagEndpointResponse.class);
    assertThat(afterResponse.getBody()).isNotNull();
    assertThat(afterResponse.getBody().features().get("new-feature")).isFalse();
  }

  @Test
  void shouldEnableFeatureFlag_whenWriteOperationCalledWithEnabledTrue() {
    var response =
        restClient
            .post()
            .uri("/actuator/feature-flags/disabled-feature")
            .contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"))
            .body("{\"enabled\":true}")
            .retrieve()
            .toEntity(Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var afterResponse =
        restClient
            .get()
            .uri("/actuator/feature-flags")
            .retrieve()
            .toEntity(FeatureFlagEndpointResponse.class);
    assertThat(afterResponse.getBody()).isNotNull();
    assertThat(afterResponse.getBody().features().get("disabled-feature")).isTrue();
  }

  FeatureFlagEndpointWriteIntegrationTest(@LocalManagementPort int port) {
    this.restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }
}
