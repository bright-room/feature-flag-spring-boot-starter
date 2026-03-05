package net.brightroom.featureflag.webflux.resolution.handlerfilter;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

/**
 * Factory for creating {@link AccessDeniedHandlerFilterResolution} instances.
 *
 * <p>Selects the appropriate resolution implementation based on the configured {@code
 * feature-flags.response.type} property.
 */
public class AccessDeniedHandlerFilterResolutionFactory {

  /**
   * Creates an {@link AccessDeniedHandlerFilterResolution} based on the response type configured in
   * the given {@link FeatureFlagProperties}.
   *
   * @param featureFlagProperties the feature flag configuration properties
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

  /** Creates a new {@code AccessDeniedHandlerFilterResolutionFactory}. */
  public AccessDeniedHandlerFilterResolutionFactory() {}
}
