package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FeatureFlagConditionController {

  @FeatureFlag(value = "conditional-feature", condition = "headers['X-Beta'] != null")
  @GetMapping("/condition/header")
  Mono<String> headerCondition() {
    return Mono.just("Allowed");
  }

  @FeatureFlag(value = "conditional-feature", condition = "params['variant'] == 'B'")
  @GetMapping("/condition/param")
  Mono<String> paramCondition() {
    return Mono.just("Allowed");
  }

  @FeatureFlag(value = "conditional-feature", condition = "headers['X-Beta'] != null", rollout = 50)
  @GetMapping("/condition/with-rollout")
  Mono<String> conditionWithRollout() {
    return Mono.just("Allowed");
  }

  @FeatureFlag(value = "conditional-feature", condition = "remoteAddress == '127.0.0.1'")
  @GetMapping("/condition/remote-address")
  Mono<String> remoteAddressCondition() {
    return Mono.just("Allowed");
  }

  public FeatureFlagConditionController() {}
}
