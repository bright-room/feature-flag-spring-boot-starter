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
  @ConditionalOnMissingBean(AccessDeniedInterceptResolution.class)
  AccessDeniedInterceptResolution featureFlagAccessDeniedResponse(
      JsonMapper jsonMapper,
      @Value("${spring.mvc.problemdetails.enabled:false}") boolean useRFC7807) {
    ResponseProperties responseProperties = featureFlagProperties.response();
    ResponseType type = responseProperties.type();

    if (type == ResponseType.PlainText)
      return new AccessDeniedInterceptResolutionViaPlainTextResponse(
          responseProperties.statusCode(), responseProperties.message());

    if (useRFC7807)
      return new AccessDeniedInterceptResolutionViaRFC7807JsonResponse(
          responseProperties.statusCode(), responseProperties.body(), jsonMapper);

    return new AccessDeniedInterceptResolutionViaJsonResponse(
        responseProperties.statusCode(), responseProperties.body(), jsonMapper);
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
