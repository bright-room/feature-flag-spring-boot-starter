package net.brightroom.featureflag.core.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for feature flag configuration.
 *
 * <p>These properties are used to define which paths are included or excluded from feature flag
 * checks and to define the default enabled status for specific features.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   path-patterns:
 *     includes:
 *       - "/api/**"
 *     excludes:
 *       - "/api/public/**"
 *   feature-names:
 *     "new-feature": true
 *     "beta-feature": false
 * }</pre>
 */
@ConfigurationProperties(prefix = "feature-flags")
public class FeatureFlagProperties {

  FeatureFlagPathPatterns pathPatterns = new FeatureFlagPathPatterns();
  Map<String, Boolean> featureNames = new ConcurrentHashMap<>();
  ResponseProperties response = new ResponseProperties();

  /**
   * Returns the path patterns for feature flag interceptor registration.
   *
   * @return the path patterns
   */
  public FeatureFlagPathPatterns pathPatterns() {
    return pathPatterns;
  }

  /**
   * Returns the map of feature names and their enabled status.
   *
   * @return the map of features
   */
  public Map<String, Boolean> featureNames() {
    return featureNames;
  }

  /**
   * Returns the response properties.
   *
   * @return the response properties
   */
  public ResponseProperties response() {
    return response;
  }

  // for property binding
  void setPathPatterns(FeatureFlagPathPatterns pathPatterns) {
    this.pathPatterns = pathPatterns;
  }

  // for property binding
  void setFeatureNames(Map<String, Boolean> featureNames) {
    this.featureNames = featureNames;
  }

  // for property binding
  void setResponse(ResponseProperties response) {
    this.response = response;
  }

  FeatureFlagProperties() {}
}
