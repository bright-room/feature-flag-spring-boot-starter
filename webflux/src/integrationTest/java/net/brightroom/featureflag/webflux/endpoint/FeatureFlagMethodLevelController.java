package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FeatureFlagMethodLevelController {

  @GetMapping("/stable-endpoint")
  String stableEndpoint() {
    return "No Annotation";
  }

  @FeatureFlag("experimental-stage-endpoint")
  @GetMapping("/experimental-stage-endpoint")
  Mono<String> experimentalStageEndpoint() {
    return Mono.just("Allowed");
  }

  @FeatureFlag("development-stage-endpoint")
  @GetMapping("/development-stage-endpoint")
  Mono<String> developmentStageEndpoint() {
    return Mono.just("Not Allowed");
  }

  public FeatureFlagMethodLevelController() {}
}
