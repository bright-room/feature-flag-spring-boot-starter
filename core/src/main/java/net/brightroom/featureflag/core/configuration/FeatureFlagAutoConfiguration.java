package net.brightroom.featureflag.core.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * {@link AutoConfiguration Auto-configuration} for feature flag support.
 *
 * <p>This configuration enables {@link FeatureFlagProperties} to be populated from the application
 * environment (e.g., {@code application.yml}).
 */
@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagAutoConfiguration {
  /** Default constructor. */
  FeatureFlagAutoConfiguration() {}
}
