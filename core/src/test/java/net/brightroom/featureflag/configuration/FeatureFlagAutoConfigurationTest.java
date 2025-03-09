package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.brightroom.featureflag.Application;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class})
class FeatureFlagAutoConfigurationTest {

  @ActiveProfiles("undefined")
  @Nested
  class UndefinedProperties {

    FeatureFlagProperties featureFlagProperties;

    @Test
    void allInitializedWithDefaultValues() {

      Map<String, Boolean> features = featureFlagProperties.features();
      assertNotNull(features);
      assertEquals(0, features.size());

      ResponseProperties response = featureFlagProperties.response();
      assertNotNull(response);

      assertEquals(405, response.status());

      ResponseBodyProperties body = response.body();
      assertNotNull(body);
      assertTrue(body.isEnabled());
      assertEquals("Feature Not Available", body.title());
      assertEquals("The requested feature is not available", body.detail());
    }

    @Autowired
    UndefinedProperties(FeatureFlagProperties featureFlagProperties) {
      this.featureFlagProperties = featureFlagProperties;
    }
  }

  @ActiveProfiles("defined")
  @Nested
  class DefinedProperties {

    FeatureFlagProperties featureFlagProperties;

    @Test
    void verifyThatThePropertyIsInPlace() {
      Map<String, Boolean> features = featureFlagProperties.features();
      assertNotNull(features);
      assertTrue(features.containsKey("new-api"));
      assertTrue(features.get("new-api"));

      assertTrue(features.containsKey("beta-feature"));
      assertFalse(features.get("beta-feature"));

      ResponseProperties response = featureFlagProperties.response();
      assertNotNull(response);

      assertEquals(500, response.status());

      ResponseBodyProperties body = response.body();
      assertNotNull(body);
      assertTrue(body.isEnabled());
      assertEquals("dummy title", body.title());
      assertEquals("dummy details", body.detail());
    }

    @Autowired
    DefinedProperties(FeatureFlagProperties featureFlagProperties) {
      this.featureFlagProperties = featureFlagProperties;
    }
  }
}
