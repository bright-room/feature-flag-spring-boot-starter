package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.brightroom.featureflag.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles("defined-features")
class DefinedFeaturesPropertiesTest {

  FeatureFlagProperties featureFlagProperties;

  @Test
  void definitionExistsInFeatures() {
    Map<String, Boolean> features = featureFlagProperties.features();

    assertTrue(features.containsKey("new-api"));
    assertTrue(features.get("new-api"));

    assertTrue(features.containsKey("beta-feature"));
    assertFalse(features.get("beta-feature"));
  }

  @Autowired
  DefinedFeaturesPropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
