# feature-flag-spring-boot-starter

Library for integrating feature flag functionality into Spring MVC and Spring WebFlux.

## Features

- Feature Flag functions can be realized with minimal configuration.
- The source destination for feature management can be easily changed.
- Supports both MVC and FebFlux.

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
    <!-- Using Spring boot starter web -->
    <dependency>
        <groupId>net.bright-room.feature-flag-spring-boot-starter</groupId>
        <artifactId>web</artifactId>
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
    
    // Using Spring boot starter web
    implementation 'net.bright-room.feature-flag-spring-boot-starter:web:${version}'

    // Using Spring boot starter webflux
    implementation 'net.bright-room.feature-flag-spring-boot-starter:webflux:${version}'
}
```

### Gradle(Kotlin)
```kotlin
dependencies {
    implementation("net.bright-room.feature-flag-spring-boot-starter:core:${version}")
    
    // Using Spring boot starter web
    implementation("net.bright-room.feature-flag-spring-boot-starter:web:${version}")

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
  features:
    hello-class: true
    user-find: false
    ...
```

Only Spring MVC requires additional interceptors.

```java
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

  FeatureFlagInterceptor featureFlagInterceptor;
  
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(featureFlagInterceptor).addPathPatterns("/**");
  }
  
  WebConfiguration(FeatureFlagInterceptor featureFlagInterceptor) {
    this.featureFlagInterceptor = featureFlagInterceptor;
  }
}
```

Add the @FeatureFlag annotation to the class or method that will be the endpoint.

```java

// HelloController.java
@RestController
@FeatureFlag(feature = "hello-class")
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
  @FeatureFlag(feature = "user-find")
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

## Contributing

Please see [the contribution guide](.github/CONTRIBUTING.md) and the [Code of conduct](.github/CODE_OF_CONDUCT.md) before contributing.