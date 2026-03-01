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
}
```

## Examples

Runnable examples can be found in [feature-flag-spring-boot-starter-examples](https://github.com/bright-room/feature-flag-spring-boot-starter-examples).

### Configurations

By default, it is available by defining the functions you want to manage in the configuration file.

```yaml
feature-flags:
  feature-names:
    hello-class: true
    user-find: false
  default-enabled: false  # false (fail-closed, default) | true (fail-open)
  response:
    type: JSON  # PLAIN_TEXT | JSON | HTML (default: JSON)
```

> **Undefined flags are blocked by default (fail-closed).** If a feature name referenced in a `@FeatureFlag` annotation is not listed under `feature-flags.feature-names`, access is denied with `403 Forbidden`. Set `feature-flags.default-enabled: true` to allow access for undefined flags instead (fail-open).

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

To change the source destination, simply implement the `FeatureFlagProvider` and register the bean.

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
import net.brightroom.featureflag.webflux.configuration.AccessDeniedHandlerFilterResolution;
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

## Contributing

Please see [the contribution guide](.github/CONTRIBUTING.md) and the [Code of conduct](.github/CODE_OF_CONDUCT.md) before contributing.
