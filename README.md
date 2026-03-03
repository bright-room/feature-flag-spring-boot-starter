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

## Documentation

| Topic | Docs |
|-------|------|
| Response Types | [docs/response-types.md](docs/response-types.md) |
| Custom Feature Flag Provider | [docs/custom-provider.md](docs/custom-provider.md) |
| Custom Access Denied Response | [docs/custom-access-denied-response.md](docs/custom-access-denied-response.md) |
| Gradual Rollout | [docs/gradual-rollout.md](docs/gradual-rollout.md) |
| Runtime Flag Management (Actuator) | [docs/actuator.md](docs/actuator.md) |

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

## Response Types

The response format when a feature flag denies access is selected by `feature-flags.response.type`. Supports `JSON` (default, RFC 7807), `PLAIN_TEXT`, and `HTML`. See [docs/response-types.md](docs/response-types.md) for details and examples.

## Custom Feature Flag Provider

By default, feature flags are managed in the configuration file. You can change the source to a database or external service by implementing `FeatureFlagProvider`. See [docs/custom-provider.md](docs/custom-provider.md) for details.

## Custom Access Denied Response

You can fully customize the response returned when a feature flag denies access. See [docs/custom-access-denied-response.md](docs/custom-access-denied-response.md) for Spring MVC and Spring WebFlux examples.

## Gradual Rollout

Use the `rollout` attribute on `@FeatureFlag` to enable a feature for only a percentage of requests. See [docs/gradual-rollout.md](docs/gradual-rollout.md) for details including sticky rollout, custom strategy, and WebFlux functional endpoints.

## Runtime Flag Management (Actuator)

The `actuator` module provides a Spring Boot Actuator endpoint for reading and updating feature flags at runtime. See [docs/actuator.md](docs/actuator.md) for setup and usage details.

## Contributing

Please see [the contribution guide](.github/CONTRIBUTING.md) and the [Code of conduct](.github/CODE_OF_CONDUCT.md) before contributing.
