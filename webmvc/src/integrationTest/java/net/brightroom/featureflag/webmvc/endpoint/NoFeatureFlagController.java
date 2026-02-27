package net.brightroom.featureflag.webmvc.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoFeatureFlagController {

  @GetMapping("/test/no-annotation")
  String noAnnotation() {
    return "No Annotation";
  }
}
