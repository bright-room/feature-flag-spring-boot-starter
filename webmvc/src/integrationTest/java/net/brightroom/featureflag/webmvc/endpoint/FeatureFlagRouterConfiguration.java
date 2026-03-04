package net.brightroom.featureflag.webmvc.endpoint;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

import net.brightroom.featureflag.webmvc.filter.FeatureFlagHandlerFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class FeatureFlagRouterConfiguration {

  private final FeatureFlagHandlerFilterFunction featureFlagFilter;

  @Bean
  RouterFunction<ServerResponse> functionalStableRoute() {
    return route(
        GET("/functional/stable-endpoint"), req -> ServerResponse.ok().body("No Annotation"));
  }

  @Bean
  RouterFunction<ServerResponse> functionalEnabledRoute() {
    return route()
        .GET("/functional/experimental-stage-endpoint", req -> ServerResponse.ok().body("Allowed"))
        .filter(featureFlagFilter.of("experimental-stage-endpoint"))
        .build();
  }

  @Bean
  RouterFunction<ServerResponse> functionalDisabledRoute() {
    return route()
        .GET(
            "/functional/development-stage-endpoint",
            req -> ServerResponse.ok().body("Not Allowed"))
        .filter(featureFlagFilter.of("development-stage-endpoint"))
        .build();
  }

  @Bean
  RouterFunction<ServerResponse> functionalClassLevelDisabledRoute() {
    return route()
        .GET("/functional/test/disable", req -> ServerResponse.ok().body("Not Allowed"))
        .filter(featureFlagFilter.of("disable-class-level-feature"))
        .build();
  }

  @Bean
  RouterFunction<ServerResponse> functionalClassLevelEnabledRoute() {
    return route()
        .GET("/functional/test/enabled", req -> ServerResponse.ok().body("Allowed"))
        .filter(featureFlagFilter.of("enable-class-level-feature"))
        .build();
  }

  @Bean
  RouterFunction<ServerResponse> functionalUndefinedFlagRoute() {
    return route()
        .GET("/functional/undefined-flag-endpoint", req -> ServerResponse.ok().body("Allowed"))
        .filter(featureFlagFilter.of("undefined-in-config-flag"))
        .build();
  }

  public FeatureFlagRouterConfiguration(FeatureFlagHandlerFilterFunction featureFlagFilter) {
    this.featureFlagFilter = featureFlagFilter;
  }
}
