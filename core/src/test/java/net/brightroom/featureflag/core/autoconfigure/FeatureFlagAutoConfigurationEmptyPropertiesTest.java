package net.brightroom.featureflag.core.autoconfigure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import net.brightroom.featureflag.core.properties.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;
import net.brightroom.featureflag.core.properties.ResponseType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {FeatureFlagAutoConfiguration.class})
@ActiveProfiles("empty")
class FeatureFlagAutoConfigurationEmptyPropertiesTest {

  FeatureFlagProperties featureFlagProperties;

  @Test
  void shouldBeEmptyWhenPropertiesAreNotProvided() {
    FeatureFlagPathPatterns pathPatterns = featureFlagProperties.pathPatterns();

    List<String> includes = pathPatterns.includes();
    assertTrue(includes.isEmpty());

    List<String> excludes = pathPatterns.excludes();
    assertTrue(excludes.isEmpty());

    Map<String, Boolean> featureNames = featureFlagProperties.featureNames();
    assertTrue(featureNames.isEmpty());

    assertFalse(featureFlagProperties.defaultEnabled());

    ResponseProperties response = featureFlagProperties.response();
    assertEquals(ResponseType.JSON, response.type());
  }

  @Autowired
  FeatureFlagAutoConfigurationEmptyPropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
