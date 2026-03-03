package net.brightroom.featureflag.webmvc.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.junit.jupiter.api.Test;

class RandomFeatureFlagContextResolverTest {

  private final RandomFeatureFlagContextResolver resolver = new RandomFeatureFlagContextResolver();
  private final HttpServletRequest request = mock(HttpServletRequest.class);

  @Test
  void resolve_returnsNonEmptyContext() {
    Optional<FeatureFlagContext> result = resolver.resolve(request);
    assertThat(result).isPresent();
  }

  @Test
  void resolve_returnsContextWithNonBlankIdentifier() {
    Optional<FeatureFlagContext> result = resolver.resolve(request);
    assertThat(result).isPresent();
    assertThat(result.get().userIdentifier()).isNotBlank();
  }

  @Test
  void resolve_returnsDifferentContextPerRequest() {
    Optional<FeatureFlagContext> first = resolver.resolve(request);
    Optional<FeatureFlagContext> second = resolver.resolve(request);
    assertThat(first).isPresent();
    assertThat(second).isPresent();
    assertThat(first.get().userIdentifier()).isNotEqualTo(second.get().userIdentifier());
  }
}
