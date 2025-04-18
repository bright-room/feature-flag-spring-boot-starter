package net.brightroom.featureflag.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for feature flags. Binds feature flag settings from the application
 * configuration using the prefix "feature-flags".
 *
 * <p>Configuration example in application.yml:
 *
 * <pre>
 * feature-flags:
 *   features:
 *     new-api: true
 *     beta-feature: false
 * </pre>
 */
@ConfigurationProperties(prefix = "feature-flags")
class FeatureFlagProperties {

  private Map<String, Boolean> features = new ConcurrentHashMap<>();
  private ResponseProperties response = new ResponseProperties();

  Map<String, Boolean> features() {
    return features;
  }

  ResponseProperties response() {
    return response;
  }

  // for property injection
  void setFeatures(Map<String, Boolean> features) {
    this.features = features;
  }

  // for property injection
  void setResponse(ResponseProperties response) {
    this.response = response;
  }

  FeatureFlagProperties() {}
}
