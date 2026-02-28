package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied in a
 * {@link HandlerFilterFunction} context.
 *
 * <p>Implementations return a {@link ServerResponse} that the functional web framework writes to
 * the client, rather than writing to the response directly.
 *
 * <p>To customize the denied response, implement this interface and register it as a {@code @Bean}.
 * The custom bean takes priority over the library's default implementation due to
 * {@code @ConditionalOnMissingBean}:
 *
 * <pre>{@code
 * @Bean
 * AccessDeniedHandlerFilterResolution myResolution() {
 *     return (request, e) -> ServerResponse.status(HttpStatus.FORBIDDEN)
 *         .bodyValue("Access denied: " + e.featureName());
 * }
 * }</pre>
 */
public interface AccessDeniedHandlerFilterResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * @param request the current server request
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return a {@link Mono} emitting the {@link ServerResponse} to send to the client
   */
  Mono<ServerResponse> resolve(ServerRequest request, FeatureFlagAccessDeniedException e);
}
