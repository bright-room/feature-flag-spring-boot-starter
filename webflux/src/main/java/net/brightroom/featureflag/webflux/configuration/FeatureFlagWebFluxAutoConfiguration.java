package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class FeatureFlagWebFluxAutoConfiguration {

  private static final Logger log =
      LoggerFactory.getLogger(FeatureFlagWebFluxAutoConfiguration.class);

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
  @ConditionalOnMissingBean
  FeatureFlagAspect featureFlagAspect(ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    return new FeatureFlagAspect(reactiveFeatureFlagProvider);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedHandlerFilterResolution.class)
  AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution() {
    return new AccessDeniedHandlerFilterResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution) {
    return new FeatureFlagHandlerFilterFunction(
        reactiveFeatureFlagProvider, accessDeniedHandlerResolution);
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
    if (featureFlagProperties.pathPatterns().hasIncludes()
        || featureFlagProperties.pathPatterns().hasExcludes()) {
      log.warn(
          "The 'feature-flags.path-patterns' configuration is set but is not supported by the "
              + "webflux module. Path-based filtering is not applicable because AOP aspects target "
              + "@FeatureFlag-annotated methods directly. "
              + "Consider removing this configuration to avoid confusion.");
    }
  }
}
