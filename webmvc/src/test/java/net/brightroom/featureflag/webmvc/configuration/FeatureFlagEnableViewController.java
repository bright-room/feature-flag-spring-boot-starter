package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
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
