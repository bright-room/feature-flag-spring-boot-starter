package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@FeatureFlag("disable-class-level-feature")
public class FeatureFlagDisableViewController {

  @GetMapping("/test/disable")
  String testDisable() {
    return "disable";
  }
}
