package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@FeatureFlag("disable-class-level-feature")
public class FeatureFlagDisableController {

  @GetMapping("/test/disable")
  Mono<String> testDisable() {
    return Mono.just("Not Allowed");
  }

  @FeatureFlag("experimental-stage-endpoint")
  @GetMapping("/test/method-override")
  Mono<String> testMethodOverride() {
    return Mono.just("Method Override Allowed");
  }
}
