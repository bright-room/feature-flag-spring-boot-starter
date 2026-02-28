package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
public class FeatureFlagWebFluxAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
