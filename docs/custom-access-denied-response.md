# Custom Access Denied Response

You can fully customize the response returned when a feature flag denies access.

## Spring MVC

Define a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.

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

## Spring WebFlux

### Annotation-based Controllers

Define a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.

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

### Functional Endpoints

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
