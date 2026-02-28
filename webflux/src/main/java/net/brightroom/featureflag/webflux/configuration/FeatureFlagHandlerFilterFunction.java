package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * A factory for {@link HandlerFilterFunction} that applies feature flag access control to
 * Functional Endpoints.
 *
 * <p>Use {@link #of(String)} to create a {@link HandlerFilterFunction} for a specific feature name
 * and apply it to a {@link org.springframework.web.reactive.function.server.RouterFunction}:
 *
 * <pre>{@code
 * @Bean
 * RouterFunction<ServerResponse> routes(FeatureFlagHandlerFilterFunction featureFlagFilter) {
 *     return route()
 *         .GET("/api/feature", handler::handle)
 *         .filter(featureFlagFilter.of("my-feature"))
 *         .build();
 * }
 * }</pre>
 *
 * <p>When the feature is disabled, the filter delegates to {@link
 * AccessDeniedHandlerFilterResolution} to build the denied response without invoking the handler.
 * The default response format follows {@code feature-flags.response.type} configuration, and can be
 * customized by providing a custom {@link AccessDeniedHandlerFilterResolution} bean.
 */
public class FeatureFlagHandlerFilterFunction {

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final AccessDeniedHandlerFilterResolution resolution;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * @param featureName the name of the feature flag to check; must not be empty
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   * @throws IllegalArgumentException if {@code featureName} is empty
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    if (featureName.isEmpty()) {
      throw new IllegalArgumentException(
          "featureName must not be empty. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    return (request, next) ->
        reactiveFeatureFlagProvider
            .isFeatureEnabled(featureName)
            .flatMap(enabled -> filterByFeatureEnabled(request, next, featureName, enabled));
  }

  private Mono<ServerResponse> filterByFeatureEnabled(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      boolean enabled) {
    if (enabled) {
      return next.handle(request);
    }
    return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
  }

  FeatureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution resolution) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.resolution = resolution;
  }
}
