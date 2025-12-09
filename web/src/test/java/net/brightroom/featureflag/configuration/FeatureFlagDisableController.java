package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FeatureFlag("disable-class-level-feature")
class FeatureFlagDisableController {

  @GetMapping("/test/disable")
  String testDisable() {
    return "Not Allowed";
  }
}
