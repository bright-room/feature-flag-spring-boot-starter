package net.brightroom.featureflag.webmvc.resolution.handlerfilter;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

/**
 * Factory for creating {@link AccessDeniedHandlerFilterResolution} instances based on the
 * configured response type.
 *
 * <p>Selects the appropriate resolution implementation from {@link
 * net.brightroom.featureflag.core.properties.ResponseType ResponseType} configured via {@code
 * feature-flags.response.type}.
 */
public class AccessDeniedHandlerFilterResolutionFactory {

  /**
   * Creates an {@link AccessDeniedHandlerFilterResolution} appropriate for the response type
   * configured in the given properties.
   *
   * @param featureFlagProperties the feature flag configuration properties; must not be null
   * @return the resolution implementation matching the configured response type
   */
  public AccessDeniedHandlerFilterResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedHandlerFilterResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedHandlerFilterResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedHandlerFilterResolutionViaJsonResponse();
    };
  }

  /** Creates a new {@link AccessDeniedHandlerFilterResolutionFactory}. */
  public AccessDeniedHandlerFilterResolutionFactory() {}
}
