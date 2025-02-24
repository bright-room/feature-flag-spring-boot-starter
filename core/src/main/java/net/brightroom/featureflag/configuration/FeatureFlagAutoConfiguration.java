package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Feature Flag functionality. Automatically sets up the feature flag system
 * with default implementations when no custom implementations are provided.
 *
 * <p>This configuration will:
 *
 * <ul>
 *   <li>Enable property binding for feature flag settings
 *   <li>Provide a default in-memory feature flag provider if no custom provider is defined
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider(FeatureFlagProperties properties) {
    return new InMemoryFeatureFlagProvider(properties.features());
  }

  FeatureFlagAutoConfiguration() {}
}
