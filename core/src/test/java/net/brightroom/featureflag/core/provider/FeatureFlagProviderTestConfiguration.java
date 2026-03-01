package net.brightroom.featureflag.core.provider;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
class FeatureFlagProviderTestConfiguration {

  @Bean
  FeatureFlagProvider featureFlagProvider(FeatureFlagProperties featureFlagProperties) {
    return new InMemoryFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }
}
