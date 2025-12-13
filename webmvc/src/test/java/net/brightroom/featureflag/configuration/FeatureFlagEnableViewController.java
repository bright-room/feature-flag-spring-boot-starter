package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.annotation.FeatureFlag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@FeatureFlag("enable-class-level-feature")
class FeatureFlagEnableViewController {

  @GetMapping("/test/enabled")
  String testEnabled() {
    return "enabled";
  }
}
