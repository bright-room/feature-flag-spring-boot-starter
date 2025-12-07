package net.brightroom.featureflag.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
class FeatureFlagAutoConfiguration {
  FeatureFlagAutoConfiguration() {}
}
