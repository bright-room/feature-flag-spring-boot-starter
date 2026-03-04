package net.brightroom.featureflag.webmvc.autoconfigure;

import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.context.RandomFeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolution;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolutionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FeatureFlagMvcAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider() {
    return new InMemoryFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  AccessDeniedInterceptResolution featureFlagAccessDeniedResponse() {
    return new AccessDeniedInterceptResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean
  RolloutStrategy rolloutStrategy() {
    return new DefaultRolloutStrategy();
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagContextResolver featureFlagContextResolver() {
    return new RandomFeatureFlagContextResolver();
  }

  @Bean
  @ConditionalOnMissingBean(RolloutPercentageProvider.class)
  RolloutPercentageProvider rolloutPercentageProvider() {
    return new InMemoryRolloutPercentageProvider(featureFlagProperties.rolloutPercentages());
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider,
      RolloutStrategy rolloutStrategy,
      FeatureFlagContextResolver contextResolver,
      RolloutPercentageProvider rolloutPercentageProvider) {
    return new FeatureFlagInterceptor(
        featureFlagProvider, rolloutStrategy, contextResolver, rolloutPercentageProvider);
  }

  @Bean
  FeatureFlagExceptionHandler featureFlagExceptionHandler(
      AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    return new FeatureFlagExceptionHandler(accessDeniedInterceptResolution);
  }

  FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
