package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.brightroom.featureflag.Application;
import net.brightroom.featureflag.response.Mode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles("undefined")
class UndefinedPropertiesTest {

  FeatureFlagProperties featureFlagProperties;

  @Test
  void DefaultFeaturesIsEmpty() {
    Map<String, Boolean> features = featureFlagProperties.features();
    assertEquals(0, features.size());
  }

  @Test
  void theDefaultStatusCodeIs405() {
    ResponseProperties response = featureFlagProperties.response();
    assertEquals(405, response.status());
  }

  @Test
  void ResponseBodyIsReturnedByDefault() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();

    assertTrue(body.isEnabled());
  }

  @Test
  void theDefaultResponseBodyIsJson() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();

    assertEquals(Mode.JSON, body.mode());
  }

  @Test
  void theDefaultJsonBodyIsValid() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();
    JsonResponseProperties json = body.json();
    DefaultJsonResponseProperties defaultJson = json.defaultFields();

    assertTrue(defaultJson.isEnabled());
  }

  @Test
  void theDefaultValueIsSetToTheContentOfTheDefaultJsonBody() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();
    JsonResponseProperties json = body.json();
    DefaultJsonResponseProperties defaultJson = json.defaultFields();

    assertEquals("Feature Not Available", defaultJson.title());
    assertEquals("The requested feature is not available", defaultJson.detail());
  }

  @Test
  void theDefaultCustomJsonBodyContentIsEmpty() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();
    JsonResponseProperties json = body.json();
    Map<String, String> customFields = json.customFields();

    assertEquals(0, customFields.size());
  }

  @Test
  void theDefaultTextMessageContentIsEmpty() {
    ResponseProperties response = featureFlagProperties.response();
    ResponseBodyProperties body = response.body();
    TextResponseProperties text = body.text();

    assertEquals("The requested feature is not available.", text.message());
  }

  @Autowired
  UndefinedPropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
