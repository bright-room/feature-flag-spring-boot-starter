package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FeatureFlag("enable-class-level-feature")
class FeatureFlagEnableController {

  @GetMapping("/test/enabled")
  String testEnabled() {
    return "Allowed";
  }
}
