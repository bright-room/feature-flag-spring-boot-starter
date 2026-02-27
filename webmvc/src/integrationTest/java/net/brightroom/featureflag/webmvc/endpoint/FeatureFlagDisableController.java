package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FeatureFlag("disable-class-level-feature")
public class FeatureFlagDisableController {

  @GetMapping("/test/disable")
  String testDisable() {
    return "Not Allowed";
  }
}
