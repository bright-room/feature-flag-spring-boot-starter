package net.brightroom.featureflag.configuration;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class NoFeatureFlagViewController {

  @GetMapping("/test/no-annotation")
  String noAnnotation() {
    return "no-annotation";
  }
}
