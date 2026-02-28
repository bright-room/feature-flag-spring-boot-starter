package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Test controller for verifying fail-closed / fail-open behavior.
 *
 * <p>The flag {@code "undefined-in-config-flag"} is intentionally absent from {@code
 * feature-flags.feature-names} in {@code application.yaml}, so its enabled state is determined
 * solely by {@code feature-flags.default-enabled}.
 */
@RestController
public class FeatureFlagUndefinedFlagController {

  @FeatureFlag("undefined-in-config-flag")
  @GetMapping("/undefined-flag-endpoint")
  Mono<String> undefinedFlagEndpoint() {
    return Mono.just("Allowed");
  }

  public FeatureFlagUndefinedFlagController() {}
}
