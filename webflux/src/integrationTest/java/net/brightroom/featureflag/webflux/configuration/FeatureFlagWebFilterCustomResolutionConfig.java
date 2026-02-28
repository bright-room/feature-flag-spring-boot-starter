package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

@Configuration
public class FeatureFlagWebFilterCustomResolutionConfig {

  @Bean
  AccessDeniedWebFilterResolution customResolution() {
    return (exchange, e) -> {
      exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
      byte[] body = ("custom: " + e.featureName()).getBytes(StandardCharsets.UTF_8);
      var buffer = exchange.getResponse().bufferFactory().wrap(body);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    };
  }
}
