package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.webmvc.provider.FeatureFlagProvider;
import net.brightroom.featureflag.webmvc.provider.InMemoryFeatureFlagProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
class FeatureFlagMvcAutoConfiguration {

  FeatureFlagProperties featureFlagProperties;

  @Bean
  AccessDeniedInterceptResolutionFactory accessDeniedInterceptResolutionFactory(
      JsonMapper jsonMapper) {
    return new AccessDeniedInterceptResolutionFactory(jsonMapper);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedInterceptResolution.class)
  AccessDeniedInterceptResolution featureFlagAccessDeniedResponse(
      AccessDeniedInterceptResolutionFactory factory,
      @Value("${spring.mvc.problemdetails.enabled:false}") boolean useRFC7807) {
    return factory.create(featureFlagProperties, useRFC7807);
  }

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider() {
    return new InMemoryFeatureFlagProvider(featureFlagProperties.featureNames());
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

  FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
