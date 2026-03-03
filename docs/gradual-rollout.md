# Gradual Rollout

Use the `rollout` attribute on `@FeatureFlag` to enable a feature for only a percentage of requests.

```java
@RestController
class BetaController {

  @GetMapping("/new-feature")
  @FeatureFlag(value = "new-feature", rollout = 50) // enable for 50% of requests
  String newFeature() {
    return "You're in the rollout!";
  }
}
```

By default, rollout is **non-sticky** — each request is evaluated independently using a random identifier. This means the same user may see different behavior across requests.

## Sticky Rollout

To make rollout sticky (the same user always gets the same result), implement a context resolver and register it as a `@Bean`.

### Spring MVC

```java
@Component
class UserBasedContextResolver implements FeatureFlagContextResolver {

  @Override
  public Optional<FeatureFlagContext> resolve(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    if (userId == null) return Optional.empty(); // fail-open: skip rollout check
    return Optional.of(new FeatureFlagContext(userId));
  }
}
```

### Spring WebFlux

```java
@Component
class UserBasedReactiveContextResolver implements ReactiveFeatureFlagContextResolver {

  @Override
  public Mono<FeatureFlagContext> resolve(ServerHttpRequest request) {
    String userId = request.getHeaders().getFirst("X-User-Id");
    if (userId == null) return Mono.empty(); // fail-open: skip rollout check
    return Mono.just(new FeatureFlagContext(userId));
  }
}
```

When the context resolver returns empty, the rollout check is skipped and the feature is treated as fully enabled (fail-open).

## Custom Rollout Strategy

To change how the rollout bucketing works, implement `RolloutStrategy` (Spring MVC) or `ReactiveRolloutStrategy` (Spring WebFlux) and register it as a `@Bean`.

## WebFlux Functional Endpoints

For functional endpoints, use `FeatureFlagHandlerFilterFunction.of(name, rollout)`:

```java
@Bean
RouterFunction<ServerResponse> routes(FeatureFlagHandlerFilterFunction featureFlagFilter) {
    return route()
        .GET("/new-feature", handler::handle)
        .filter(featureFlagFilter.of("new-feature", 50))
        .build();
}
```
