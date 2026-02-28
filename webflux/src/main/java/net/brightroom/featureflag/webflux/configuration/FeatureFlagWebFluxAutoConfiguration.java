package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class FeatureFlagWebFluxAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  AccessDeniedExceptionHandlerResolution accessDeniedExceptionHandlerResolution() {
    return new AccessDeniedExceptionHandlerResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  FeatureFlagExceptionHandler featureFlagExceptionHandler(
      AccessDeniedExceptionHandlerResolution accessDeniedExceptionHandlerResolution) {
    return new FeatureFlagExceptionHandler(accessDeniedExceptionHandlerResolution);
  }

  @Bean
  FeatureFlagAspect featureFlagAspect(ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    return new FeatureFlagAspect(reactiveFeatureFlagProvider);
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
