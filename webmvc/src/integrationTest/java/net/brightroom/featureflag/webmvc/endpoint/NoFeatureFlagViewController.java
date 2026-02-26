package net.brightroom.featureflag.webmvc.endpoint;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NoFeatureFlagViewController {

  @GetMapping("/test/no-annotation")
  String noAnnotation() {
    return "no-annotation";
  }
}
