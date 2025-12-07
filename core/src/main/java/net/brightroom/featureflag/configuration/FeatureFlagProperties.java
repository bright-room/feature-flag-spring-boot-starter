package net.brightroom.featureflag.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for feature flags. Binds feature flag settings from the application
 * configuration using the prefix "feature-flags".
 *
 * <p>Configuration example in application.yaml:
 *
 * <pre>
 * feature-flags:
 *   include-path-pattern:
 *     - /api/v1/feature-flag-enabled
 *   exclude-path-pattern:
 *     - /api/v1/feature-flag-disabled
 *   features:
 *     new-api: true
 *     beta-feature: false
 *   response:
 *     status-code: 403
 *     message: "This feature is not available"
 * </pre>
 */
@ConfigurationProperties(prefix = "feature-flags")
class FeatureFlagProperties {

  List<String> includePathPattern = new ArrayList<>();
  List<String> excludePathPattern = new ArrayList<>();
  Map<String, Boolean> features = new ConcurrentHashMap<>();
  ResponseProperties response = new ResponseProperties();

  List<String> includePathPattern() {
    return includePathPattern;
  }

  List<String> excludePathPattern() {
    return excludePathPattern;
  }

  Map<String, Boolean> features() {
    return features;
  }

  ResponseProperties response() {
    return response;
  }

  // for property injection
  void setIncludePathPattern(List<String> includePathPattern) {
    this.includePathPattern = includePathPattern;
  }

  // for property injection
  void setExcludePathPattern(List<String> excludePathPattern) {
    this.excludePathPattern = excludePathPattern;
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
