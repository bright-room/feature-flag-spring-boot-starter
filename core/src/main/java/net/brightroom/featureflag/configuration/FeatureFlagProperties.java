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
public class FeatureFlagProperties {

  private Map<String, Boolean> features = new ConcurrentHashMap<>();

  Map<String, Boolean> features() {
    return features;
  }

  // for property injection
  void setFeatures(Map<String, Boolean> features) {
    this.features = features;
  }

  FeatureFlagProperties() {}
}
