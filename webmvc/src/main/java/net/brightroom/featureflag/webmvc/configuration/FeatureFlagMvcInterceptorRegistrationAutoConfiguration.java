package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
class FeatureFlagMvcInterceptorRegistrationAutoConfiguration {

  FeatureFlagMvcInterceptorRegistrationAutoConfiguration() {}

  @Configuration(proxyBeanMethods = false)
  static class MvcConfigurer implements WebMvcConfigurer {

    private final FeatureFlagInterceptor featureFlagInterceptor;
    private final FeatureFlagProperties featureFlagProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      InterceptorRegistration registration = registry.addInterceptor(featureFlagInterceptor);

      FeatureFlagPathPatterns featureFlagPathPatterns = featureFlagProperties.pathPatterns();

      if (featureFlagPathPatterns.hasIncludes()) {
        registration.addPathPatterns(featureFlagPathPatterns.includes());
      }

      if (featureFlagPathPatterns.hasExcludes()) {
        registration.excludePathPatterns(featureFlagPathPatterns.excludes());
      }
    }

    MvcConfigurer(
        FeatureFlagInterceptor featureFlagInterceptor,
        FeatureFlagProperties featureFlagProperties) {
      this.featureFlagInterceptor = featureFlagInterceptor;
      this.featureFlagProperties = featureFlagProperties;
    }
  }
}
