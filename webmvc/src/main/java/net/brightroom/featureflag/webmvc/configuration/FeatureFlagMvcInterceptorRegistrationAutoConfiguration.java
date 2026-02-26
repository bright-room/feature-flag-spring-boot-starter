package net.brightroom.featureflag.webmvc.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
class FeatureFlagMvcInterceptorRegistrationAutoConfiguration implements WebMvcConfigurer {

  FeatureFlagInterceptor featureFlagInterceptor;
  FeatureFlagInterceptorRegistrationRule featureFlagInterceptorRegistrationRule;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    InterceptorRegistration registration = registry.addInterceptor(featureFlagInterceptor);

    if (featureFlagInterceptorRegistrationRule.isNotEmptyIncludePathPattern()) {
      registration.addPathPatterns(featureFlagInterceptorRegistrationRule.includePathPattern());
    }

    if (featureFlagInterceptorRegistrationRule.isNotEmptyExcludePathPattern()) {
      registration.excludePathPatterns(featureFlagInterceptorRegistrationRule.excludePathPattern());
    }
  }

  FeatureFlagMvcInterceptorRegistrationAutoConfiguration(
      FeatureFlagInterceptor featureFlagInterceptor,
      FeatureFlagInterceptorRegistrationRule featureFlagInterceptorRegistrationRule) {
    this.featureFlagInterceptor = featureFlagInterceptor;
    this.featureFlagInterceptorRegistrationRule = featureFlagInterceptorRegistrationRule;
  }
}
