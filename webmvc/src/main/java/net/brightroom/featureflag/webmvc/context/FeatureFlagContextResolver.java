package net.brightroom.featureflag.webmvc.context;

import jakarta.servlet.http.HttpServletRequest;
import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Strategy interface for resolving the {@link FeatureFlagContext} from the current HTTP request.
 *
 * <p>The resolved context supplies the user identifier that drives hash-based rollout bucket
 * assignment. Implementations are responsible for extracting an appropriate, stable identifier from
 * the request — for example a session ID, a JWT subject claim, or a custom header value.
 *
 * <p>The default implementation ({@link SessionIdFeatureFlagContextResolver}) uses the HTTP session
 * ID. To customise the resolution strategy, declare a {@code @Bean} of this type; it will
 * automatically replace the default via {@code @ConditionalOnMissingBean}.
 *
 * <p>Example (header-based):
 *
 * <pre>{@code
 * @Bean
 * public FeatureFlagContextResolver featureFlagContextResolver() {
 *     return request -> new FeatureFlagContext(request.getHeader("X-User-Id"));
 * }
 * }</pre>
 */
public interface FeatureFlagContextResolver {

  /**
   * Resolves the feature flag context from the given HTTP request.
   *
   * @param request the current HTTP servlet request; never {@code null}
   * @return a {@link FeatureFlagContext} containing a non-blank user identifier; never {@code null}
   */
  FeatureFlagContext resolve(HttpServletRequest request);
}
