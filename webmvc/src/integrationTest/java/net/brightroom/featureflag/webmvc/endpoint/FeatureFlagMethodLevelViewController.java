package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FeatureFlagMethodLevelViewController {

  @GetMapping("/stable")
  String stableEndpoint() {
    return "stable";
  }

  @FeatureFlag("experimental-stage-endpoint")
  @GetMapping("/experimental-stage")
  String experimentalStageEndpoint() {
    return "experimental-stage";
  }

  @FeatureFlag("development-stage-endpoint")
  @GetMapping("/development-stage")
  String developmentStageEndpoint() {
    return "development-stage";
  }

  public FeatureFlagMethodLevelViewController() {}
}
