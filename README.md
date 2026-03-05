# feature-flag-spring-boot-starter

Library for integrating feature flag functionality into Spring MVC and Spring WebFlux.

## Features

- Feature Flag functions can be realized with minimal configuration.
- The source destination for feature management can be easily changed.
- Supports both MVC and WebFlux.

## Installation

See the [release notes](https://github.com/bright-room/feature-flag-spring-boot-starter/releases) for available versions.

### Apache Maven

```xml
<dependencies>
    <dependency>
        <groupId>net.bright-room.feature-flag-spring-boot-starter</groupId>
        <artifactId>core</artifactId>
        <version>${version}</version>
    </dependency>
    <!-- Using Spring boot starter webmvc -->
    <dependency>
        <groupId>net.bright-room.feature-flag-spring-boot-starter</groupId>
        <artifactId>webmvc</artifactId>
        <version>${version}</version>
    </dependency>

    <!-- Using Spring boot starter webflux -->
    <dependency>
        <groupId>net.bright-room.feature-flag-spring-boot-starter</groupId>
        <artifactId>webflux</artifactId>
        <version>${version}</version>
    </dependency>

    <!-- Runtime flag management via Actuator (optional) -->
    <dependency>
        <groupId>net.bright-room.feature-flag-spring-boot-starter</groupId>
        <artifactId>actuator</artifactId>
        <version>${version}</version>
    </dependency>
</dependencies>
```

### Gradle(Groovy)
```groovy
dependencies {
    implementation 'net.bright-room.feature-flag-spring-boot-starter:core:${version}'

    // Using Spring boot starter webmvc
    implementation 'net.bright-room.feature-flag-spring-boot-starter:webmvc:${version}'

    // Using Spring boot starter webflux
    implementation 'net.bright-room.feature-flag-spring-boot-starter:webflux:${version}'

    // Runtime flag management via Actuator (optional)
    implementation 'net.bright-room.feature-flag-spring-boot-starter:actuator:${version}'
}
```

### Gradle(Kotlin)
```kotlin
dependencies {
    implementation("net.bright-room.feature-flag-spring-boot-starter:core:${version}")

    // Using Spring boot starter webmvc
    implementation("net.bright-room.feature-flag-spring-boot-starter:webmvc:${version}")

    // Using Spring boot starter webflux
    implementation("net.bright-room.feature-flag-spring-boot-starter:webflux:${version}")

    // Runtime flag management via Actuator (optional)
    implementation("net.bright-room.feature-flag-spring-boot-starter:actuator:${version}")
}
```

## Examples

Runnable examples can be found in [feature-flag-spring-boot-starter-examples](https://github.com/bright-room/feature-flag-spring-boot-starter-examples).

### Configurations

By default, it is available by defining the functions you want to manage in the configuration file.

```yaml
feature-flags:
  features:
    hello-class:
      enabled: true
    user-find:
      enabled: false
      rollout: 50
  default-enabled: false  # false (fail-closed, default) | true (fail-open)
  response:
    type: JSON  # PLAIN_TEXT | JSON | HTML (default: JSON)
```

> **Undefined flags are blocked by default (fail-closed).** If a feature name referenced in a `@FeatureFlag` annotation is not listed under `feature-flags.features`, access is denied with `403 Forbidden`. Set `feature-flags.default-enabled: true` to allow access for undefined flags instead (fail-open).

Add the `@FeatureFlag` annotation to the class or method that will be the endpoint.

```java

// HelloController.java
@RestController
@FeatureFlag(value = "hello-class")
class HelloController {

  @GetMapping("/hello")
  String hello() {
    return "Hello world!!";
  }
}

// UserController.java
@RestController
class UserController {

  UserService userService;

  @GetMapping("/find")
  @FeatureFlag(value = "user-find")
  UserResponse find(@RequestParam("name") String name) {
    return userService.find(name);
  }

  UserController(UserService userService) {
    this.userService = userService;
  }
}
```

## Change the source destination for function management

By default, function management can be set in the configuration file, but it is also possible to change the source destination.

By changing the source of function management to a database, external file, etc., it is possible to control in real time.

To change the source destination, simply implement the `FeatureFlagProvider` (Spring MVC) or `ReactiveFeatureFlagProvider` (Spring WebFlux) and register the bean.

### Spring MVC

```java

// FeatureFlagExternalDataSourceProvider.java
@Component
class FeatureFlagExternalDataSourceProvider implements FeatureFlagProvider {

  FeatureManagementMapper featureManagementMapper;

  @Override
  public boolean isFeatureEnabled(String featureName) {
    Boolean enabled = featureManagementMapper.check(featureName);
    // Choose your undefined-flag policy:
    //   return false; — fail-closed: block access for undefined flags (recommended)
    //   return true; — fail-open: allow access for undefined flags
    if (enabled == null) return false;
    return enabled;
  }

  FeatureFlagExternalDataSourceProvider(FeatureManagementMapper featureManagementMapper) {
    this.featureManagementMapper = featureManagementMapper;
  }
}

// FeatureManagementMapper.java
@Mapper
interface FeatureManagementMapper {
  Boolean check(@Param("feature") String feature);
}
```

### Spring WebFlux

```java
@Component
class ReactiveFeatureFlagExternalDataSourceProvider implements ReactiveFeatureFlagProvider {

  FeatureManagementRepository featureManagementRepository;

  @Override
  public Mono<Boolean> isFeatureEnabled(String featureName) {
    return featureManagementRepository.findByFeatureName(featureName)
        .map(FeatureManagement::enabled)
        // Choose your undefined-flag policy:
        //   Mono.just(false) — fail-closed (recommended)
        //   Mono.just(true) — fail-open
        .defaultIfEmpty(false);
  }

  ReactiveFeatureFlagExternalDataSourceProvider(FeatureManagementRepository featureManagementRepository) {
    this.featureManagementRepository = featureManagementRepository;
  }
}
```

## Response Types

When a feature flag is disabled, `FeatureFlagAccessDeniedException` is thrown and the response is returned with HTTP status `403 Forbidden`. The response format is selected by `feature-flags.response.type`.

### JSON Response (default)

JSON responses follow the [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) format.

```yaml
feature-flags:
  response:
    type: JSON
```

Response body:

```json
{
  "type": "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types",
  "title": "Feature flag access denied",
  "detail": "Feature 'user-find' is not available",
  "status": 403,
  "instance": "/api/v2/find"
}
```

### Plain Text Response

```yaml
feature-flags:
  response:
    type: PLAIN_TEXT
```

Response body:

```
Feature 'user-find' is not available
```

### HTML Response

```yaml
feature-flags:
  response:
    type: HTML
```

> **Note (Spring MVC only):** The HTML response is returned only when the client's `Accept` header includes `text/html` or `text/*`. If the client only accepts `application/json`, a `406 Not Acceptable` response is returned instead. In Spring WebFlux, the HTML response is always returned regardless of the `Accept` header.

## Custom Access Denied Response

### Spring MVC

You can create a fully custom response by defining a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.

```java
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// CustomFeatureFlagExceptionHandler.java
@ControllerAdvice
@Order(0) // Ensure this handler takes priority over the library's default handler
public class CustomFeatureFlagExceptionHandler {

  @ExceptionHandler(FeatureFlagAccessDeniedException.class)
  public ResponseEntity<String> handle(FeatureFlagAccessDeniedException e) {
    return ResponseEntity.status(403)
        .contentType(MediaType.TEXT_PLAIN)
        .body("Feature '" + e.featureName() + "' is disabled.");
  }
}
```

### Spring WebFlux (Annotation-based controllers)

You can create a fully custom response by defining a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.

```java
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// CustomFeatureFlagExceptionHandler.java
@ControllerAdvice
@Order(0) // Ensure this handler takes priority over the library's default handler
public class CustomFeatureFlagExceptionHandler {

  @ExceptionHandler(FeatureFlagAccessDeniedException.class)
  ResponseEntity<String> handle(FeatureFlagAccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body("Feature '" + e.featureName() + "' is disabled.");
  }
}
```

### Spring WebFlux (Functional endpoints)

Define an `AccessDeniedHandlerFilterResolution` bean to customize the response returned by the `HandlerFilterFunction`.

```java
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

// CustomHandlerFilterResolutionConfig.java
@Configuration
public class CustomHandlerFilterResolutionConfig {

  @Bean
  AccessDeniedHandlerFilterResolution customResolution() {
    return (request, e) -> ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue("Feature '" + e.featureName() + "' is disabled.");
  }
}
```

## Gradual Rollout

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

You can also configure the rollout percentage in `application.yml` without changing the annotation:

```yaml
feature-flags:
  features:
    new-feature:
      enabled: true
      rollout: 50  # enable for 50% of requests (0–100, default: 100)
```

When `feature-flags.features.*.rollout` is configured, it takes priority over the `@FeatureFlag(rollout = ...)` annotation value. The annotation value is used as a fallback when no config-based rollout is defined. The config value can also be overridden at runtime via the [Actuator endpoint](#runtime-flag-management-actuator).

By default, rollout is **non-sticky** — each request is evaluated independently using a random identifier. This means the same user may see different behavior across requests.

### Sticky Rollout

To make rollout sticky (the same user always gets the same result), implement `FeatureFlagContextResolver` (Spring MVC) or `ReactiveFeatureFlagContextResolver` (Spring WebFlux) and register it as a `@Bean`.

```java
// Spring MVC
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

```java
// Spring WebFlux
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

### Custom Rollout Strategy

To change how the rollout bucketing works, implement `RolloutStrategy` (Spring MVC) or `ReactiveRolloutStrategy` (Spring WebFlux) and register it as a `@Bean`.

### WebFlux Functional Endpoints

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

## Runtime Flag Management (Actuator)

The `actuator` module provides a Spring Boot Actuator endpoint for reading and updating feature flags at runtime without restarting the application.

### Setup

1. Add the `actuator` dependency (see [Installation](#installation)).
2. Expose the endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: feature-flags
```

### Read all flags

```
GET /actuator/feature-flags
```

Response:

```json
{
  "features": [
    { "featureName": "hello-class", "enabled": true, "rollout": 100 },
    { "featureName": "user-find", "enabled": false, "rollout": 50 }
  ],
  "defaultEnabled": false
}
```

### Read a single flag

```
GET /actuator/feature-flags/{featureName}
```

Response:

```json
{
  "featureName": "user-find",
  "enabled": false,
  "rollout": 100
}
```

If the flag is not defined, `enabled` reflects the `defaultEnabled` policy and `rollout` is `100`.

### Update a flag

```
POST /actuator/feature-flags
Content-Type: application/json

{
  "featureName": "user-find",
  "enabled": true,
  "rollout": 50
}
```

The `rollout` field is optional (0–100). If omitted, the rollout percentage is left unchanged.

Response:

```json
{
  "features": [
    { "featureName": "hello-class", "enabled": true, "rollout": 100 },
    { "featureName": "user-find", "enabled": true, "rollout": 50 }
  ],
  "defaultEnabled": false
}
```

If the flag does not exist, it is created with the given state.

### Delete a flag

```
DELETE /actuator/feature-flags/{featureName}
```

Removes the feature flag and its associated rollout percentage. Returns `204 No Content`.

This operation is idempotent: deleting a non-existent flag is a no-op and still returns `204 No Content`. A [`FeatureFlagRemovedEvent`](#event-integration) is published only if the flag actually existed.

### Restricting access

By default, both read and write operations are unrestricted. In production, consider restricting access:

```yaml
management:
  endpoint:
    feature-flags:
      access: READ_ONLY
```

Or secure the endpoint with Spring Security.

### Event integration

The following events are published via the actuator endpoint:

| Event | Trigger |
|-------|---------|
| `FeatureFlagChangedEvent` | A flag is created or updated via `POST /actuator/feature-flags` |
| `FeatureFlagRemovedEvent` | An existing flag is deleted via `DELETE /actuator/feature-flags/{featureName}` |

Subscribe with `@EventListener` to react to changes (e.g., clearing caches, logging audit trails).

> **WebFlux (reactive) environments:** Events are published synchronously on the calling thread, which may be the Netty event loop thread. Listeners must not perform blocking operations directly; use `@Async` or subscribe on `Schedulers.boundedElastic()` to offload blocking work.

```java
@Component
class FeatureFlagChangeListener {

  @EventListener
  void onFlagChanged(FeatureFlagChangedEvent event) {
    log.info("Flag '{}' changed to {}", event.featureName(), event.enabled());
  }

  @EventListener
  void onFlagRemoved(FeatureFlagRemovedEvent event) {
    log.info("Flag '{}' removed", event.featureName());
  }
}
```

## Contributing

Please see [the contribution guide](.github/CONTRIBUTING.md) and the [Code of conduct](.github/CODE_OF_CONDUCT.md) before contributing.
