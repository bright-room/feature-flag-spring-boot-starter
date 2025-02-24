package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.filter.FeatureFlagWebFilter;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Autoconfiguration for Feature Flag WebFlux support. Configures the Feature Flag web filter for
 * reactive applications. This configuration is loaded after the main Feature Flag
 * autoconfiguration.
 */
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
public class FeatureFlagReactiveAutoConfiguration {

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
