# feature-flag-spring-boot-starter

Library for integrating feature flag functionality into Spring MVC.

## Features

- Feature Flag functions can be realized with minimal configuration.
- The source destination for feature management can be easily changed.
- Supports both MVC.

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
</dependencies>
```

### Gradle(Groovy)
```groovy
dependencies {
    implementation 'net.bright-room.feature-flag-spring-boot-starter:core:${version}'
    
    // Using Spring boot starter webmvc
    implementation 'net.bright-room.feature-flag-spring-boot-starter:webmvc:${version}'
}
```

### Gradle(Kotlin)
```kotlin
dependencies {
    implementation("net.bright-room.feature-flag-spring-boot-starter:core:${version}")
    
    // Using Spring boot starter webmvc
    implementation("net.bright-room.feature-flag-spring-boot-starter:webmvc:${version}")
}
```

## Examples

Runnable examples can be found in [feature-flag-spring-boot-starter-examples](https://github.com/bright-room/feature-flag-spring-boot-starter-examples).

### Configurations

By default, it is available by defining the functions you want to manage in the configuration file.

```yaml
feature-flags:
  include-path-pattern:
    - "/api/v2/**"
  exclude-path-pattern:
    - "/api/v2/foo"
    - "/api/v2/bar"
    - "/api/v1/**"
  features:
    hello-class: true
    user-find: false
  response:
    status-code: 405
#    type: PlainText
#    message: "This feature is disabled."
    type: Json
    body:
      error: "Feature flag is disabled"
```

Add the @FeatureFlag annotation to the class or method that will be the endpoint.

```java

// HelloController.java
@RestController
@FeatureFlag(value = "hello-class")
class HelloController {

  @GetMapping("/hello")
  String hello() {
    return "Hello world!!";
  }

  HelloController() {}
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

To change the source destination, simply implement the FeatureFlagProvider and register the bean.

```java

// FeatureFlagExternalDataSourceProvider.java
@Component
class FeatureFlagExternalDataSourceProvider implements FeatureFlagProvider {

  FeatureManagementMapper featureManagementMapper;

  @Override
  public boolean isFeatureEnabled(String featureName) {
    Boolean enabled = featureManagementMapper.check(featureName);
    if (enabled == null) return true;
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

## Custom Access Denied Response

The library provides two default response types: `Json` and `PlainText`. You can create a custom response by implementing the `FeatureFlagAccessDeniedResponse` interface.

### Example: XML Response

Here's an example of creating a custom XML response:

```java
import java.util.Map;

// CustomXmlResponse.java
public class CustomXmlResponse implements FeatureFlagAccessDeniedResponse {

  Integer statusCode;
  Map<String, String> body;

  CustomXmlResponse(
          Integer statusCode, Map<String, String> body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  @Override
  public boolean supportsDirectWrite() {
    return false;
  }

  @Override
  public ModelAndView toModelAndView() {
    JacksonXmlView o = new JacksonXmlView();
    o.setAttributesMap(body);

    ModelAndView modelAndView = new ModelAndView();
    modelAndView.setStatus(HttpStatusCode.valueOf(statusCode));

    return modelAndView;
  }
}

// Configuration.java
@Bean
public FeatureFlagAccessDeniedResponse customXmlResponse() {
  return new CustomXmlResponse(403, Map.of("error", "Feature flag is disabled"));
}
```

## Contributing

Please see [the contribution guide](.github/CONTRIBUTING.md) and the [Code of conduct](.github/CODE_OF_CONDUCT.md) before contributing.