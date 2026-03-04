package net.brightroom.featureflag.webmvc.resolution.handlerfilter;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

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
 *         .body("Access denied: " + e.featureName());
 * }
 * }</pre>
 */
public interface AccessDeniedHandlerFilterResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * @param request the current server request
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return the {@link ServerResponse} to send to the client
   */
  ServerResponse resolve(ServerRequest request, FeatureFlagAccessDeniedException e);
}
