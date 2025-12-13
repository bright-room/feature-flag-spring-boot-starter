package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
class FeatureFlagMvcAutoConfiguration {

  FeatureFlagProperties featureFlagProperties;

  @Bean
  FeatureFlagInterceptorRegistrationRule featureFlagInterceptorRegistrationRule() {
    return new FeatureFlagInterceptorRegistrationRule(
        featureFlagProperties.includePathPattern(), featureFlagProperties.excludePathPattern());
  }

  @Bean
  AccessDeniedInterceptResolutionFactory accessDeniedInterceptResolutionFactory(
      JsonMapper jsonMapper,
      @Value("${spring.mvc.problemdetails.enabled:false}") boolean useRFC7807) {
    return new AccessDeniedInterceptResolutionFactory(jsonMapper, useRFC7807);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedInterceptResolution.class)
  AccessDeniedInterceptResolution featureFlagAccessDeniedResponse(
      AccessDeniedInterceptResolutionFactory factory) {
    return factory.create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider() {
    return new InMemoryFeatureFlagProvider(featureFlagProperties.features());
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider,
      AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    return new FeatureFlagInterceptor(featureFlagProvider, accessDeniedInterceptResolution);
  }

  FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
