package net.brightroom.featureflag.webmvc.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import({
  FeatureFlagMvcAutoConfiguration.class,
  FeatureFlagMvcInterceptorRegistrationAutoConfiguration.class
})
public class FeatureFlagMvcTestAutoConfiguration {

  @Primary
  @Bean
  JsonMapper jsonMapper() {
    return JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .changeDefaultVisibility(
            handler -> {
              handler.withFieldVisibility(JsonAutoDetect.Visibility.NON_PRIVATE);
              handler.withGetterVisibility(JsonAutoDetect.Visibility.NONE);
              handler.withSetterVisibility(JsonAutoDetect.Visibility.NONE);

              return handler;
            })
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();
  }
}
