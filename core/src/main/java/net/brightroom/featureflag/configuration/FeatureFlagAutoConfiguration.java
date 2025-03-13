package net.brightroom.featureflag.configuration;

import java.util.Map;
import net.brightroom.featureflag.response.AccessDeniedResponse;
import net.brightroom.featureflag.response.Mode;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Feature Flag functionality. Automatically sets up the feature flag system
 * with default implementations when no custom implementations are provided.
 *
 * <p>This configuration will:
 *
 * <ul>
 *   <li>Enable property binding for feature flag settings
 *   <li>Provide a default in-memory feature flag provider if no custom provider is defined
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AccessDeniedResponse.Builder.class)
  AccessDeniedResponse.Builder builder(FeatureFlagProperties properties) {
    ResponseProperties response = properties.response();
    ResponseBodyProperties body = response.body();

    AccessDeniedResponse.Builder builder =
        AccessDeniedResponse.newBuilder(body.isEnabled(), body.mode()).status(response.status());

    if (!body.isEnabled()) return builder;

    Mode mode = body.mode();
    if (mode.isText()) {
      TextResponseProperties text = body.text();
      builder = builder.append(text.message());

      return builder;
    }

    JsonResponseProperties json = body.json();
    DefaultJsonResponseProperties defaultJson = json.defaultFields();

    if (defaultJson.isEnabled()) {
      builder = builder.append("title", defaultJson.title()).append("detail", defaultJson.detail());
    }

    Map<String, String> customFields = json.customFields();
    for (String key : customFields.keySet()) {
      String value = customFields.get(key);
      builder = builder.append(key, value);
    }

    return builder;
  }

  FeatureFlagAutoConfiguration() {}
}
