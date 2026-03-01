package net.brightroom.featureflag.webflux;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    properties = {"feature-flags.response.type=HTML"},
    controllers = {
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagAspectHtmlResponseIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() {
    String html =
        webTestClient
            .get()
            .uri("/development-stage-endpoint")
            .exchange()
            .expectStatus()
            .isForbidden()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    Document doc = Jsoup.parse(html);
    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'development-stage-endpoint' is not available", doc.select("p").text());
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() {
    String html =
        webTestClient
            .get()
            .uri("/test/disable")
            .exchange()
            .expectStatus()
            .isForbidden()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    Document doc = Jsoup.parse(html);
    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'disable-class-level-feature' is not available", doc.select("p").text());
  }

  @Autowired
  FeatureFlagAspectHtmlResponseIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
