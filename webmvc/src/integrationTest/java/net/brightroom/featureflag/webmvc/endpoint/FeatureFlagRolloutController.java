package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for verifying gradual rollout behaviour.
 *
 * <p>All methods reference {@code "rollout-test-feature"}, which is set to {@code true} in {@code
 * application.yaml}. The {@code rollout} attribute on each method exercises a different rollout
 * scenario.
 */
@RestController
@RequestMapping("/rollout")
public class FeatureFlagRolloutController {

  @FeatureFlag(value = "rollout-test-feature", rollout = 100)
  @GetMapping("/full")
  String full() {
    return "Allowed (rollout=100)";
  }

  @FeatureFlag(value = "rollout-test-feature", rollout = 0)
  @GetMapping("/none")
  String none() {
    return "Allowed (rollout=0)";
  }

  @FeatureFlag(value = "rollout-test-feature", rollout = 50)
  @GetMapping("/partial")
  String partial() {
    return "Allowed (rollout=50)";
  }

  public FeatureFlagRolloutController() {}
}
