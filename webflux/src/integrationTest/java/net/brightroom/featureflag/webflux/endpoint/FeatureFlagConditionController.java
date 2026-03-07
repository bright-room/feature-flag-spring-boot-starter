package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FeatureFlagConditionController {

  @FeatureFlag("header-condition-feature")
  @GetMapping("/condition/header")
  Mono<String> headerCondition() {
    return Mono.just("Allowed");
  }

  @FeatureFlag("param-condition-feature")
  @GetMapping("/condition/param")
  Mono<String> paramCondition() {
    return Mono.just("Allowed");
  }

  @FeatureFlag("condition-rollout-feature")
  @GetMapping("/condition/with-rollout")
  Mono<String> conditionWithRollout() {
    return Mono.just("Allowed");
  }

  @FeatureFlag("remote-address-condition-feature")
  @GetMapping("/condition/remote-address")
  Mono<String> remoteAddressCondition() {
    return Mono.just("Allowed");
  }

  public FeatureFlagConditionController() {}
}
