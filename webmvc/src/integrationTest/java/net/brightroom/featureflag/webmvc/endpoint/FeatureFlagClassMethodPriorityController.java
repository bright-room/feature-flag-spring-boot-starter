package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/legacy")
@FeatureFlag("legacy-api")
public class FeatureFlagClassMethodPriorityController {

  @GetMapping("/data")
  public String data() {
    return "Legacy data";
  }

  @FeatureFlag("special-endpoint")
  @GetMapping("/special")
  public String special() {
    return "Special endpoint data";
  }
}
