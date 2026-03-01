package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagRouterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;

@WebFluxTest
@Import({
  FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest.CustomResolutionConfiguration
      .class,
  FeatureFlagWebFluxTestAutoConfiguration.class,
  FeatureFlagRouterConfiguration.class,
})
class FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest {

  @Configuration
  static class CustomResolutionConfiguration {

    @Bean
    @Primary
    AccessDeniedHandlerFilterResolution customResolution() {
      return (request, e) ->
          ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
              .bodyValue("custom: " + e.featureName());
    }
  }

  WebTestClient webTestClient;

  @Test
  void customResolutionTakesPriority_whenFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/functional/development-stage-endpoint")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .isEqualTo("custom: development-stage-endpoint");
  }

  @Test
  void customResolutionTakesPriority_whenClassLevelFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/functional/test/disable")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .isEqualTo("custom: disable-class-level-feature");
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
