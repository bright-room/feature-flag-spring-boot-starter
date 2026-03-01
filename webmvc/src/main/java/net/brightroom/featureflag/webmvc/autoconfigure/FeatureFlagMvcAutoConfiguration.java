package net.brightroom.featureflag.webmvc.autoconfigure;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
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
  FeatureFlagInterceptor featureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    return new FeatureFlagInterceptor(featureFlagProvider);
  }

  @Bean
  FeatureFlagExceptionHandler featureFlagExceptionHandler(
      AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    return new FeatureFlagExceptionHandler(accessDeniedInterceptResolution);
  }

  public FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
