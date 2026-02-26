package net.brightroom.featureflag.core.configuration;

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
 *     - "/api/v2/**"
 *   exclude-path-pattern:
 *     - "/api/v2/foo"
 *     - "/api/v2/bar"
 *     - "/api/v1/**"
 *   features:
 *     hello-class: true
 *     user-find: false
 *   response:
 * #   status-code: 405
 * #   message: "This feature is disabled."
 *   type: JSON
 *   body:
 *     error: "Feature flag is disabled"
 * </pre>
 */
@ConfigurationProperties(prefix = "feature-flags")
public class FeatureFlagProperties {

  List<String> includePathPattern = new ArrayList<>();
  List<String> excludePathPattern = new ArrayList<>();
  Map<String, Boolean> features = new ConcurrentHashMap<>();
  ResponseProperties response = new ResponseProperties();

  public List<String> includePathPattern() {
    return includePathPattern;
  }

  public List<String> excludePathPattern() {
    return excludePathPattern;
  }

  public Map<String, Boolean> features() {
    return features;
  }

  public ResponseProperties response() {
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
