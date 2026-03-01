package net.brightroom.featureflag.webmvc.autoconfigure;

import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.context.SessionIdFeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolution;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolutionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
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
  @ConditionalOnMissingBean(FeatureFlagContextResolver.class)
  FeatureFlagContextResolver featureFlagContextResolver() {
    return new SessionIdFeatureFlagContextResolver();
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider, FeatureFlagContextResolver contextResolver) {
    return new FeatureFlagInterceptor(featureFlagProvider, contextResolver);
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
