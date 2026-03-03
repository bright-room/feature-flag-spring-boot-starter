package net.brightroom.featureflag.webflux.autoconfigure;

import java.util.UUID;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.properties.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webflux.aspect.FeatureFlagAspect;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolution;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolutionFactory;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolutionFactory;
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
public class FeatureFlagWebFluxAutoConfiguration {

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
  RolloutStrategy rolloutStrategy() {
    return new DefaultRolloutStrategy();
  }

  @Bean
  @ConditionalOnMissingBean
  ReactiveFeatureFlagContextResolver reactiveFeatureFlagContextResolver() {
    return request ->
        reactor.core.publisher.Mono.just(new FeatureFlagContext(UUID.randomUUID().toString()));
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagAspect featureFlagAspect(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      RolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver) {
    return new FeatureFlagAspect(reactiveFeatureFlagProvider, rolloutStrategy, contextResolver);
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
      AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution,
      RolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver) {
    return new FeatureFlagHandlerFilterFunction(
        reactiveFeatureFlagProvider,
        accessDeniedHandlerResolution,
        rolloutStrategy,
        contextResolver);
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;

    @SuppressWarnings("deprecation")
    FeatureFlagPathPatterns pathPatterns = featureFlagProperties.pathPatterns();
    if (pathPatterns.hasIncludes() || pathPatterns.hasExcludes()) {
      log.warn(
          "The 'feature-flags.path-patterns' configuration is set but is not supported by the "
              + "webflux module. Path-based filtering is not applicable because AOP aspects target "
              + "@FeatureFlag-annotated methods directly. "
              + "Consider removing this configuration to avoid confusion.");
    }
  }
}
