package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FeatureFlagMethodLevelController {

  @GetMapping("/stable-endpoint")
  String stableEndpoint() {
    return "No Annotation";
  }

  @FeatureFlag("experimental-stage-endpoint")
  @GetMapping("/experimental-stage-endpoint")
  String experimentalStageEndpoint() {
    return "Allowed";
  }

  @FeatureFlag("development-stage-endpoint")
  @GetMapping("/development-stage-endpoint")
  String developmentStageEndpoint() {
    return "Not Allowed";
  }

  FeatureFlagMethodLevelController() {}
}
