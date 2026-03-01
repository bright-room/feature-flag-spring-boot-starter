package net.brightroom.featureflag.webmvc.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Default {@link FeatureFlagContextResolver} that uses the HTTP session ID as the user identifier.
 *
 * <p>When an active session exists its ID is used as the {@code userId}, providing sticky rollout
 * behaviour across requests within the same session. When no session exists a random UUID is
 * generated per request, which means rollout decisions will <em>not</em> be sticky for anonymous
 * requests.
 *
 * <p>For stateless APIs (e.g. JWT-based or header-based authentication), replace this bean with a
 * custom {@link FeatureFlagContextResolver} implementation that extracts a stable user identifier
 * from the request.
 *
 * <p>This bean is registered with {@code @ConditionalOnMissingBean}, so declaring any other {@link
 * FeatureFlagContextResolver} {@code @Bean} will automatically disable this default.
 */
public class SessionIdFeatureFlagContextResolver implements FeatureFlagContextResolver {

  @Override
  public FeatureFlagContext resolve(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    String userId = (session != null) ? session.getId() : UUID.randomUUID().toString();
    return new FeatureFlagContext(userId);
  }
}
