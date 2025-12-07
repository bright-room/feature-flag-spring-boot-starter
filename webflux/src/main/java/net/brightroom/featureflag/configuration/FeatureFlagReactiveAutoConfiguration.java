package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
class FeatureFlagReactiveAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider(FeatureFlagProperties properties) {
    return new InMemoryFeatureFlagProvider(properties.features());
  }

  @Bean
  FeatureFlagWebFilter featureFlagWebFilter(
      FeatureFlagProvider featureFlagProvider, RequestMappingHandlerMapping handlerMapping) {
    return new FeatureFlagWebFilter(featureFlagProvider, handlerMapping);
  }

  FeatureFlagReactiveAutoConfiguration() {}
}
