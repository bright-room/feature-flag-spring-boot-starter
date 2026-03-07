package net.brightroom.featureflag.core.properties;

import java.util.HashMap;
import java.util.Map;
import net.brightroom.featureflag.core.provider.Schedule;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for feature flag configuration.
 *
 * <p>These properties are used to define the enabled status and rollout percentage for specific
 * features.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   features:
 *     new-feature:
 *       enabled: true
 *       rollout: 50
 *     beta-feature:
 *       enabled: true
 *       rollout: 25
 *     simple-feature:
 *       enabled: true
 * }</pre>
 */
@ConfigurationProperties(prefix = "feature-flags")
public class FeatureFlagProperties {

  private Map<String, FeatureProperties> features = new HashMap<>();
  private ResponseProperties response = new ResponseProperties();
  private ConditionProperties condition = new ConditionProperties();
  private boolean defaultEnabled = false;

  /**
   * Returns a map of feature names and their enabled status, derived from {@code features}.
   *
   * <p>This is a convenience view for components that only need enabled/disabled status (e.g.,
   * {@code FeatureFlagProvider} initialization).
   *
   * @return an immutable map of feature names to their enabled states
   */
  public Map<String, Boolean> featureNames() {
    var result = new HashMap<String, Boolean>();
    features.forEach((name, config) -> result.put(name, config.enabled()));
    return Map.copyOf(result);
  }

  /**
   * Returns a map of feature names and their rollout percentages, derived from {@code features}.
   *
   * @return an immutable map of feature names to their rollout percentages
   */
  public Map<String, Integer> rolloutPercentages() {
    var result = new HashMap<String, Integer>();
    features.forEach((name, config) -> result.put(name, config.rollout()));
    return Map.copyOf(result);
  }

  /**
   * Returns a map of feature names to their condition expressions. Features without a condition
   * (empty string) are excluded.
   *
   * @return an immutable map of feature names to their condition expressions
   */
  public Map<String, String> conditions() {
    var result = new HashMap<String, String>();
    features.forEach(
        (name, config) -> {
          if (!config.condition().isEmpty()) {
            result.put(name, config.condition());
          }
        });
    return Map.copyOf(result);
  }

  /**
   * Returns a map of feature names to their schedule value objects. Features without a schedule are
   * excluded.
   *
   * @return an immutable map of feature names to their {@link Schedule}
   */
  public Map<String, Schedule> schedules() {
    var result = new HashMap<String, Schedule>();
    features.forEach(
        (name, config) -> {
          if (config.schedule() != null) {
            result.put(name, config.schedule().toSchedule());
          }
        });
    return Map.copyOf(result);
  }

  /**
   * Returns the full feature configuration map.
   *
   * @return an immutable map of feature names to their {@link FeatureProperties}
   */
  public Map<String, FeatureProperties> features() {
    return Map.copyOf(features);
  }

  /**
   * Returns the response properties.
   *
   * @return the response properties
   */
  public ResponseProperties response() {
    return response;
  }

  /**
   * Returns the condition evaluation properties.
   *
   * @return the condition properties
   */
  public ConditionProperties condition() {
    return condition;
  }

  /**
   * Returns whether undefined feature flags are enabled by default.
   *
   * @return {@code true} if undefined flags are enabled (fail-open), {@code false} if disabled
   *     (fail-closed)
   */
  public boolean defaultEnabled() {
    return defaultEnabled;
  }

  // for property binding
  void setFeatures(Map<String, FeatureProperties> features) {
    this.features = features;
  }

  // for property binding
  void setResponse(ResponseProperties response) {
    this.response = response;
  }

  // for property binding
  void setCondition(ConditionProperties condition) {
    this.condition = condition;
  }

  // for property binding
  void setDefaultEnabled(boolean defaultEnabled) {
    this.defaultEnabled = defaultEnabled;
  }

  FeatureFlagProperties() {}
}
