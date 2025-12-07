package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
class FeatureFlagMvcAutoConfiguration {

  FeatureFlagProperties featureFlagProperties;

  @Bean
  FeatureFlagInterceptorRegistrationRule featureFlagInterceptorRegistrationRule() {
    return new FeatureFlagInterceptorRegistrationRule(
        featureFlagProperties.includePathPattern(), featureFlagProperties.excludePathPattern());
  }

  @Bean
  @ConditionalOnMissingBean(FeatureFlagAccessDeniedResponse.class)
  FeatureFlagAccessDeniedResponse featureFlagAccessDeniedResponse() {
    ResponseProperties responseProperties = featureFlagProperties.response();
    ResponseType type = responseProperties.type();

    if (type == ResponseType.PlainText)
      return new FeatureFlagAccessDeniedPlainTextResponse(
          responseProperties.statusCode(), responseProperties.message());

    return new FeatureFlagAccessDeniedJsonResponse(
        responseProperties.statusCode(), responseProperties.body());
  }

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider() {
    return new InMemoryFeatureFlagProvider(featureFlagProperties.features());
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    return new FeatureFlagInterceptor(featureFlagProvider);
  }

  FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
