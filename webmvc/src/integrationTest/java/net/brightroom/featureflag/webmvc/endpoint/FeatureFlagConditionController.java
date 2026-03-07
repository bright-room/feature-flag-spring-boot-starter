package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureFlagConditionController {

  @FeatureFlag("header-condition-feature")
  @GetMapping("/condition/header")
  String headerCondition() {
    return "Allowed";
  }

  @FeatureFlag("param-condition-feature")
  @GetMapping("/condition/param")
  String paramCondition() {
    return "Allowed";
  }

  @FeatureFlag("condition-rollout-feature")
  @GetMapping("/condition/with-rollout")
  String conditionWithRollout() {
    return "Allowed";
  }

  @FeatureFlag("remote-address-condition-feature")
  @GetMapping("/condition/remote-address")
  String remoteAddressCondition() {
    return "Allowed";
  }

  public FeatureFlagConditionController() {}
}
