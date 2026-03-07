package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@FeatureFlag("rollout-feature")
public class FeatureFlagClassRolloutController {

  @GetMapping("/test/class-rollout")
  public Mono<String> testClassRollout() {
    return Mono.just("Allowed");
  }

  public FeatureFlagClassRolloutController() {}
}
