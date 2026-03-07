package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.junit.jupiter.api.Test;

class AccessDecisionTest {

  @Test
  void allowed_returnsAllowedInstance() {
    AccessDecision decision = AccessDecision.allowed();
    assertThat(decision).isInstanceOf(AccessDecision.Allowed.class);
  }

  @Test
  void denied_returnsDeniedInstanceWithFeatureNameAndReason() {
    AccessDecision decision = AccessDecision.denied("my-feature", DeniedReason.DISABLED);
    assertThat(decision).isInstanceOf(AccessDecision.Denied.class);
    AccessDecision.Denied denied = (AccessDecision.Denied) decision;
    assertThat(denied.featureName()).isEqualTo("my-feature");
    assertThat(denied.reason()).isEqualTo(DeniedReason.DISABLED);
  }

  @Test
  void denied_supportsAllReasons() {
    for (DeniedReason reason : DeniedReason.values()) {
      AccessDecision decision = AccessDecision.denied("feature", reason);
      assertThat(((AccessDecision.Denied) decision).reason()).isEqualTo(reason);
    }
  }
}
