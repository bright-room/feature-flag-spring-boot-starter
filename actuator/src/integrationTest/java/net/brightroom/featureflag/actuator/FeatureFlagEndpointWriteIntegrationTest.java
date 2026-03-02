package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FeatureFlagEndpointWriteIntegrationTest.EventCaptor.class)
@TestPropertySource(
    properties = {
      "management.endpoints.web.exposure.include=feature-flags",
      "feature-flags.feature-names.new-feature=true",
      "feature-flags.feature-names.disabled-feature=false"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeatureFlagEndpointWriteIntegrationTest {

  @TestComponent
  static class EventCaptor {
    final List<FeatureFlagChangedEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    void onChanged(FeatureFlagChangedEvent event) {
      events.add(event);
    }
  }

  @Autowired EventCaptor eventCaptor;

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

    assertThat(eventCaptor.events).hasSize(1);
    assertThat(eventCaptor.events.get(0).featureName()).isEqualTo("new-feature");
    assertThat(eventCaptor.events.get(0).enabled()).isFalse();
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

    assertThat(eventCaptor.events).hasSize(1);
    assertThat(eventCaptor.events.get(0).featureName()).isEqualTo("disabled-feature");
    assertThat(eventCaptor.events.get(0).enabled()).isTrue();
  }

  FeatureFlagEndpointWriteIntegrationTest(@LocalManagementPort int port) {
    this.restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }
}
