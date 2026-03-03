package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FeatureFlagRolloutController {

  @GetMapping("/test/rollout")
  @FeatureFlag(value = "rollout-feature", rollout = 50)
  public Mono<String> testRollout() {
    return Mono.just("Allowed");
  }

  public FeatureFlagRolloutController() {}
}
