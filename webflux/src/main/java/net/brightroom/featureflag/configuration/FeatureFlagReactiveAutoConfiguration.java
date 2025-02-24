package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.filter.FeatureFlagWebFilter;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Autoconfiguration for Feature Flag WebFlux support. Configures the Feature Flag web filter for
 * reactive applications. This configuration is loaded after the main Feature Flag
 * autoconfiguration.
 */
@AutoConfigureAfter(FeatureFlagAutoConfiguration.class)
public class FeatureFlagReactiveAutoConfiguration {

  @Bean
  FeatureFlagWebFilter featureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider, RequestMappingHandlerMapping handlerMapping) {
    return new FeatureFlagWebFilter(featureFlagProvider, handlerMapping);
  }

  FeatureFlagReactiveAutoConfiguration() {}
}
