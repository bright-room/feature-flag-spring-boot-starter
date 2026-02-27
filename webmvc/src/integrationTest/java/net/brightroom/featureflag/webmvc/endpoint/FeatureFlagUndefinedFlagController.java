package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
  String undefinedFlagEndpoint() {
    return "Allowed";
  }

  public FeatureFlagUndefinedFlagController() {}
}
