package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
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
class FeatureFlagEndpointIntegrationTest {

  TestRestTemplate restTemplate;

  @Test
  void shouldReturnOk_whenGetEndpointCalled() {
    var response = restTemplate.getForEntity("/actuator/feature-flags", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturnAllFeatureFlags_whenGetEndpointCalled() {
    var response =
        restTemplate.getForEntity("/actuator/feature-flags", FeatureFlagEndpointResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().features())
        .containsExactlyInAnyOrderEntriesOf(Map.of("new-feature", true, "disabled-feature", false));
  }

  @Autowired
  FeatureFlagEndpointIntegrationTest(TestRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
}
