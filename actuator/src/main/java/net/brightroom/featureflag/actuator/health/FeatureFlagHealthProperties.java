package net.brightroom.featureflag.actuator.health;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the feature flag health indicator.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * management:
 *   health:
 *     feature-flag:
 *       timeout: 5s
 * }</pre>
 */
@ConfigurationProperties(prefix = "management.health.feature-flag")
public class FeatureFlagHealthProperties {

  /**
   * Maximum time to wait for the feature flag provider to respond during a health check. When the
   * provider does not respond within this duration, the health status is reported as {@code DOWN}.
   * If not set, there is no timeout.
   */
  private Duration timeout;

  /**
   * Returns the health check timeout.
   *
   * @return the timeout duration, or {@code null} if no timeout is configured
   */
  public Duration timeout() {
    return timeout;
  }

  // for property binding
  void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  FeatureFlagHealthProperties() {}
}
