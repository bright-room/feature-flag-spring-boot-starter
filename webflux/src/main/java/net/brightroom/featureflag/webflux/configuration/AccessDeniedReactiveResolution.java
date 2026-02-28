package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied in a
 * reactive context.
 *
 * <p>The {@link AccessDeniedReactiveResolution} interface serves as a strategy to determine how
 * HTTP requests should be resolved when access is restricted due to a disabled feature flag.
 * Implementations write the denial response directly to the {@link
 * org.springframework.http.server.reactive.ServerHttpResponse} in a non-blocking manner.
 *
 * <p>Implementations of this interface can define various resolutions such as returning JSON, plain
 * text, or HTML responses, depending on the application's requirements.
 */
interface AccessDeniedReactiveResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * <p>Implementations write the response directly to {@link ServerWebExchange#getResponse()}.
   * Returning the resulting {@link Mono} signals that the response has been fully written and the
   * filter chain should not be continued.
   *
   * @param exchange the current server exchange
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return a {@link Mono<Void>} that completes when the response has been written
   */
  Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e);
}
