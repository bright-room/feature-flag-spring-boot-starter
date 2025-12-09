package net.brightroom.featureflag.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

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
  ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
    objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

    return objectMapper;
  }

  @Bean
  HttpMessageConverters customConvertor() {
    HttpMessageConverter<?> stringHttpMessageConverter =
        new StringHttpMessageConverter(StandardCharsets.UTF_8);
    HttpMessageConverter<?> mappingJackson2HttpMessageConverter =
        new MappingJackson2HttpMessageConverter(objectMapper());
    return new HttpMessageConverters(
        stringHttpMessageConverter, mappingJackson2HttpMessageConverter);
  }
}
