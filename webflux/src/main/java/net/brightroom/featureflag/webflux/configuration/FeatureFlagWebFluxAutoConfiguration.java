package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
public class FeatureFlagWebFluxAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedReactiveResolution.class)
  AccessDeniedReactiveResolution accessDeniedReactiveResolution(JsonMapper objectMapper) {
    return new AccessDeniedReactiveResolutionFactory(objectMapper).create(featureFlagProperties);
  }

  @Bean
  FeatureFlagWebFilter featureFlagWebFilter(
      RequestMappingHandlerMapping requestMappingHandlerMapping,
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedReactiveResolution accessDeniedReactiveResolution) {
    return new FeatureFlagWebFilter(
        requestMappingHandlerMapping, reactiveFeatureFlagProvider, accessDeniedReactiveResolution);
  }

  @Bean
  FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    return new FeatureFlagHandlerFilterFunction(reactiveFeatureFlagProvider, featureFlagProperties);
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
