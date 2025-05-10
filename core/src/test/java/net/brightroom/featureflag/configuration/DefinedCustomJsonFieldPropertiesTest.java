package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.brightroom.featureflag.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles("defined-custom-json-field")
class DefinedCustomJsonFieldPropertiesTest {

  FeatureFlagProperties featureFlagProperties;

  @Test
  void definitionExistsInCustomJsonField() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();
    JsonResponseProperties json = body.json();
    Map<String, String> customFields = json.customFields();

    assertTrue(customFields.containsKey("test_message"));
    assertEquals("test message", customFields.get("test_message"));
  }

  @Autowired
  DefinedCustomJsonFieldPropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
