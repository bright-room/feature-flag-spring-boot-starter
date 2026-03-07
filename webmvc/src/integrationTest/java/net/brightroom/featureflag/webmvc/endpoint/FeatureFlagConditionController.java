package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureFlagConditionController {

  @FeatureFlag(value = "conditional-feature", condition = "headers['X-Beta'] != null")
  @GetMapping("/condition/header")
  String headerCondition() {
    return "Allowed";
  }

  @FeatureFlag(value = "conditional-feature", condition = "params['variant'] == 'B'")
  @GetMapping("/condition/param")
  String paramCondition() {
    return "Allowed";
  }

  @FeatureFlag(value = "conditional-feature", condition = "headers['X-Beta'] != null", rollout = 50)
  @GetMapping("/condition/with-rollout")
  String conditionWithRollout() {
    return "Allowed";
  }

  public FeatureFlagConditionController() {}
}
