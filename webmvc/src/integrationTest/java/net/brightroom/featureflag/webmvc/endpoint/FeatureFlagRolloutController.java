package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureFlagRolloutController {

  @GetMapping("/test/rollout")
  @FeatureFlag("rollout-feature")
  public String testRollout() {
    return "Allowed";
  }
}
