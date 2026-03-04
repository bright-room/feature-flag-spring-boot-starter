package net.brightroom.featureflag.webflux;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Verifies rollout behavior for Functional Endpoints through the full WebFlux stack with a real
 * Netty server:
 *
 * <ul>
 *   <li>Custom {@link ReactiveFeatureFlagContextResolver} bean is respected via
 *       {@code @ConditionalOnMissingBean}, enabling sticky rollout.
 *   <li>Rollout decision is deterministic for a fixed user identifier.
 *   <li>{@link FeatureFlagHandlerFilterFunction#of(String, int)} correctly applies rollout control
 *       via {@code ServerRequest.exchange().getRequest()} in the real Netty pipeline.
 * </ul>
 *
 * <p>A real Netty server is used (instead of {@code @WebFluxTest}) to ensure the full Spring
 * WebFlux pipeline — including context propagation of {@code ServerWebExchange} — is exercised.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"feature-flags.features.rollout-feature.enabled=true"})
class FeatureFlagHandlerFilterFunctionRolloutIntegrationTest {

  private static final FeatureFlagContext FIXED_CONTEXT = new FeatureFlagContext("fixed-user-id");
  private static final boolean IN_ROLLOUT_50 =
      new DefaultRolloutStrategy().isInRollout("rollout-feature", FIXED_CONTEXT, 50);

  @TestConfiguration
  static class FixedContextResolverConfig {
    @Bean
    ReactiveFeatureFlagContextResolver contextResolver() {
      return request -> Mono.just(FIXED_CONTEXT);
    }
  }

  @TestConfiguration
  static class RolloutRouteConfig {
    @Bean
    RouterFunction<ServerResponse> functionalRolloutTestRoute(
        FeatureFlagHandlerFilterFunction featureFlagFilter) {
      return route()
          .GET("/functional/rollout-test", req -> ServerResponse.ok().bodyValue("Allowed"))
          .filter(featureFlagFilter.of("rollout-feature", 50))
          .build();
    }
  }

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void rollout_returnsDeterministicResult_forFixedUserId() {
    if (IN_ROLLOUT_50) {
      webTestClient.get().uri("/functional/rollout-test").exchange().expectStatus().isOk();
    } else {
      webTestClient.get().uri("/functional/rollout-test").exchange().expectStatus().isForbidden();
    }
  }

  @Test
  void rollout_sameUserAlwaysGetsSameResult() {
    // Call twice — result must be identical (deterministic hashing)
    for (int i = 0; i < 2; i++) {
      if (IN_ROLLOUT_50) {
        webTestClient.get().uri("/functional/rollout-test").exchange().expectStatus().isOk();
      } else {
        webTestClient.get().uri("/functional/rollout-test").exchange().expectStatus().isForbidden();
      }
    }
  }

  @Test
  void rollout_returnsBody_whenInRollout() {
    Assumptions.assumeTrue(IN_ROLLOUT_50, "User not in rollout bucket");
    webTestClient
        .get()
        .uri("/functional/rollout-test")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }
}
