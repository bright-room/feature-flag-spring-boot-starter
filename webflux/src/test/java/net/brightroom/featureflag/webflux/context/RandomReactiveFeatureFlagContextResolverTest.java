package net.brightroom.featureflag.webflux.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.test.StepVerifier;

class RandomReactiveFeatureFlagContextResolverTest {

  private final RandomReactiveFeatureFlagContextResolver resolver =
      new RandomReactiveFeatureFlagContextResolver();
  private final ServerHttpRequest request = mock(ServerHttpRequest.class);

  @Test
  void resolve_returnsNonEmptyContext() {
    StepVerifier.create(resolver.resolve(request))
        .assertNext(ctx -> assertThat(ctx).isNotNull())
        .verifyComplete();
  }

  @Test
  void resolve_returnsContextWithNonBlankIdentifier() {
    StepVerifier.create(resolver.resolve(request))
        .assertNext(ctx -> assertThat(ctx.userIdentifier()).isNotBlank())
        .verifyComplete();
  }

  @Test
  void resolve_returnsDifferentContextPerRequest() {
    FeatureFlagContext first = resolver.resolve(request).block();
    FeatureFlagContext second = resolver.resolve(request).block();
    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(first.userIdentifier()).isNotEqualTo(second.userIdentifier());
  }
}
