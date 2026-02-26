package net.brightroom.featureflag.webmvc.configuration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NoFeatureFlagController {

  @GetMapping("/test/no-annotation")
  String noAnnotation() {
    return "No Annotation";
  }
}
