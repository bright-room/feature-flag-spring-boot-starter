package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FeatureFlag("rollout-feature")
public class FeatureFlagClassRolloutController {

  @GetMapping("/test/class-rollout")
  public String testClassRollout() {
    return "Allowed";
  }
}
