package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagEnableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import net.brightroom.featureflag.webflux.endpoint.NoFeatureFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    controllers = {
      NoFeatureFlagController.class,
      FeatureFlagEnableController.class,
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagWebFilterJsonResponseIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenNoAnnotated() {
    webTestClient
        .get()
        .uri("/stable-endpoint")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("No Annotation");
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() {
    webTestClient
        .get()
        .uri("/experimental-stage-endpoint")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/development-stage-endpoint")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail" : "Feature 'development-stage-endpoint' is not available",
              "instance" : "/development-stage-endpoint",
              "status" : 403,
              "title" : "Feature flag access denied",
              "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Test
  void shouldAllowAccess_whenNoFeatureFlagAnnotationOnController() {
    webTestClient
        .get()
        .uri("/test/no-annotation")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("No Annotation");
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/test/disable")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail" : "Feature 'disable-class-level-feature' is not available",
              "instance" : "/test/disable",
              "status" : 403,
              "title" : "Feature flag access denied",
              "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Test
  void shouldAllowAccess_whenClassLevelFeatureIsEnabled() {
    webTestClient
        .get()
        .uri("/test/enabled")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Test
  void shouldAllowAccess_whenMethodAnnotationOverridesClassAnnotation() {
    webTestClient
        .get()
        .uri("/test/method-override")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Method Override Allowed");
  }

  @Autowired
  FeatureFlagWebFilterJsonResponseIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
