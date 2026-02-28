package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied in a
 * {@link org.springframework.web.server.WebFilter} context.
 *
 * <p>Implementations write the denial response directly to the {@link
 * org.springframework.http.server.reactive.ServerHttpResponse} in a non-blocking manner and return
 * {@link Mono}{@code <Void>} to signal completion.
 *
 * <p>To customize the denied response, implement this interface and register it as a {@code @Bean}.
 * The custom bean takes priority over the library's default implementation due to
 * {@code @ConditionalOnMissingBean}:
 *
 * <pre>{@code
 * @Bean
 * AccessDeniedWebFilterResolution myResolution() {
 *     return (exchange, e) -> {
 *         ServerHttpResponse response = exchange.getResponse();
 *         response.setStatusCode(HttpStatus.FORBIDDEN);
 *         // write custom body ...
 *     };
 * }
 * }</pre>
 */
public interface AccessDeniedWebFilterResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * <p>Implementations write the response directly to {@link ServerWebExchange#getResponse()}.
   * Returning the resulting {@link Mono} signals that the response has been fully written and the
   * filter chain should not be continued.
   *
   * @param exchange the current server exchange
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return a {@link Mono}{@code <Void>} that completes when the response has been written
   */
  Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e);
}
