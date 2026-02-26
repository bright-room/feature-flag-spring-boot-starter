package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
class FeatureFlagMvcInterceptorRegistrationAutoConfiguration implements WebMvcConfigurer {

  FeatureFlagInterceptor featureFlagInterceptor;
  FeatureFlagProperties featureFlagProperties;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    InterceptorRegistration registration = registry.addInterceptor(featureFlagInterceptor);

    FeatureFlagPathPatterns featureFlagPathPatterns = featureFlagProperties.pathPatterns();

    if (featureFlagPathPatterns.isNotEmptyIncludes()) {
      registration.addPathPatterns(featureFlagPathPatterns.includes());
    }

    if (featureFlagPathPatterns.isNotEmptyExcludes()) {
      registration.excludePathPatterns(featureFlagPathPatterns.excludes());
    }
  }

  FeatureFlagMvcInterceptorRegistrationAutoConfiguration(
      FeatureFlagInterceptor featureFlagInterceptor, FeatureFlagProperties featureFlagProperties) {
    this.featureFlagInterceptor = featureFlagInterceptor;
    this.featureFlagProperties = featureFlagProperties;
  }
}
