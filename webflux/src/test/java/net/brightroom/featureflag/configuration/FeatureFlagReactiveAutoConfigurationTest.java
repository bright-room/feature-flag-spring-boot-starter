package net.brightroom.featureflag.configuration;

import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.restassured.RestAssured;
import net.brightroom.featureflag.Application;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = {
      Application.class,
      FeatureFlagReactiveAutoConfiguration.class,
    },
    webEnvironment = RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureFlagReactiveAutoConfigurationTest {

  @LocalServerPort Integer port;

  @BeforeAll
  void setUp() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void undefinedFeaturesSreAccessible() {
    given().when().get("/exist-endpoint").then().assertThat().statusCode(200);
  }

  @Test
  void ifAccessIsAllowedWithAPredefinedFeature() {
    given().when().get("/new-endpoint").then().assertThat().statusCode(200);
  }

  @Test
  void ifAccessIsDeniedForAPredefinedFeature() {
    given().when().get("/beta-endpoint").then().assertThat().statusCode(405);
  }
}
