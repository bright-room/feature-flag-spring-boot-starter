package net.brightroom.featureflag.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {FeatureFlagAutoConfiguration.class})
@TestPropertySource(properties = {"spring.config.location=classpath:/application-empty.yaml"})
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

    ResponseProperties response = featureFlagProperties.response();
    assertEquals(ResponseType.JSON, response.type());
  }

  @Autowired
  FeatureFlagAutoConfigurationEmptyPropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
