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
class FeatureFlagWebFluxAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedWebFilterResolution.class)
  AccessDeniedWebFilterResolution accessDeniedReactiveResolution(JsonMapper objectMapper) {
    return new AccessDeniedWebFilterResolutionFactory(objectMapper).create(featureFlagProperties);
  }

  @Bean
  FeatureFlagWebFilter featureFlagWebFilter(
      RequestMappingHandlerMapping requestMappingHandlerMapping,
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedWebFilterResolution accessDeniedReactiveResolution) {
    return new FeatureFlagWebFilter(
        requestMappingHandlerMapping, reactiveFeatureFlagProvider, accessDeniedReactiveResolution);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedHandlerFilterResolution.class)
  AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution() {
    return new AccessDeniedHandlerFilterResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution) {
    return new FeatureFlagHandlerFilterFunction(
        reactiveFeatureFlagProvider, accessDeniedHandlerResolution);
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
