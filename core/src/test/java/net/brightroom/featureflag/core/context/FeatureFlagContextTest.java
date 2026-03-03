package net.brightroom.featureflag.core.context;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FeatureFlagContextTest {

  @Test
  void constructor_throwsNullPointerException_whenUserIdentifierIsNull() {
    assertThatNullPointerException()
        .isThrownBy(() -> new FeatureFlagContext(null))
        .withMessageContaining("userIdentifier");
  }

  @Test
  void constructor_throwsIllegalArgumentException_whenUserIdentifierIsBlank() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FeatureFlagContext("  "))
        .withMessageContaining("userIdentifier");
  }

  @Test
  void constructor_throwsIllegalArgumentException_whenUserIdentifierIsEmpty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FeatureFlagContext(""))
        .withMessageContaining("userIdentifier");
  }

  @Test
  void constructor_success_whenUserIdentifierIsValid() {
    FeatureFlagContext context = new FeatureFlagContext("user-123");
    assertEquals("user-123", context.userIdentifier());
  }
}
