package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

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

  TestRestTemplate restTemplate;

  @Test
  void shouldDisableFeatureFlag_whenWriteOperationCalledWithEnabledFalse() {
    var headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"));
    var request = new HttpEntity<>("{\"enabled\":false}", headers);

    var response =
        restTemplate.postForEntity("/actuator/feature-flags/new-feature", request, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var afterResponse =
        restTemplate.getForEntity("/actuator/feature-flags", FeatureFlagEndpointResponse.class);
    assertThat(afterResponse.getBody()).isNotNull();
    assertThat(afterResponse.getBody().features().get("new-feature")).isFalse();
  }

  @Test
  void shouldEnableFeatureFlag_whenWriteOperationCalledWithEnabledTrue() {
    var headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"));
    var request = new HttpEntity<>("{\"enabled\":true}", headers);

    var response =
        restTemplate.postForEntity("/actuator/feature-flags/disabled-feature", request, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var afterResponse =
        restTemplate.getForEntity("/actuator/feature-flags", FeatureFlagEndpointResponse.class);
    assertThat(afterResponse.getBody()).isNotNull();
    assertThat(afterResponse.getBody().features().get("disabled-feature")).isTrue();
  }

  @Autowired
  FeatureFlagEndpointWriteIntegrationTest(TestRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
}
