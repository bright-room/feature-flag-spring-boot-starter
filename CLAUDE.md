# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# Run unit tests for a specific module
./gradlew :core:test
./gradlew :webmvc:test

# Run integration tests for a specific module
./gradlew :webmvc:integrationTest

# Run a single integration test class
./gradlew :webmvc:integrationTest --tests "net.brightroom.featureflag.webmvc.FeatureFlagInterceptorJsonResponseIntegrationTest"

# Run all checks (Spotless + unit tests + integration tests)
./gradlew check

# Apply Google Java Format
./gradlew spotlessApply
```

Code formatting uses Google Java Format via Spotless. Always run `spotlessApply` before committing, or the CI `check` task will fail.

## Architecture

This is a multi-module Gradle project (Java 25, Spring Boot 4.x) that provides feature flag support for Spring MVC and Spring WebFlux applications. It is published to Maven Central under group `net.bright-room.feature-flag-spring-boot-starter`.

### Modules

- **`core`** — Annotation, configuration properties, provider SPI, and shared resolution logic. Contains `@FeatureFlag` annotation, `FeatureFlagProperties` (`feature-flags.*` config prefix), `FeatureFlagProvider`/`MutableFeatureFlagProvider` SPI (sync), `ReactiveFeatureFlagProvider`/`MutableReactiveFeatureFlagProvider` SPI (reactive, `reactor-core` is `compileOnly`), and `ProblemDetailBuilder`/`HtmlResponseBuilder`. `FeatureFlagAutoConfiguration` bootstraps property binding only (no provider beans).
- **`webmvc`** — Spring MVC interceptor implementation. Depends on `core`. Registers `InMemoryFeatureFlagProvider` bean via `FeatureFlagMvcAutoConfiguration`.
- **`webflux`** — Spring WebFlux AOP + HandlerFilterFunction implementation. Depends on `core`. Uses `ReactiveFeatureFlagProvider` and `FeatureFlagAspect` for annotation-based controllers, `FeatureFlagHandlerFilterFunction` for functional endpoints.
- **`actuator`** — Runtime feature flag management via Spring Boot Actuator endpoint (`/actuator/feature-flags`). Auto-configuration is split into `ServletConfiguration` (registers `MutableInMemoryFeatureFlagProvider` + `FeatureFlagEndpoint`) and `ReactiveConfiguration` (registers `MutableInMemoryReactiveFeatureFlagProvider` + `ReactiveFeatureFlagEndpoint`) via `@ConditionalOnWebApplication`. Publishes `FeatureFlagChangedEvent` on flag changes. Auto-configured before webmvc/webflux.
  - `GET /actuator/feature-flags` — returns all flags: `{ "features": [{ "featureName": "x", "enabled": true }, ...], "defaultEnabled": false }`
  - `GET /actuator/feature-flags/{featureName}` — returns a single flag: `{ "featureName": "x", "enabled": true }`. If the flag is not defined, `enabled` reflects the `defaultEnabled` policy.
  - `POST /actuator/feature-flags` (body: `{ "featureName": "x", "enabled": true }`) — updates a flag and returns the full flags response. **Breaking change from pre-#164**: `features` was `Map<String, Boolean>` and is now `List<{ featureName, enabled }>` to support the individual-flag endpoint.
- **`gradle-scripts`** — Composite build providing convention plugins: `spring-boot-starter`, `publish-plugin`, `spotless-java`, `spotless-kotlin`, `integration-test`.

### Request Flow

1. `FeatureFlagMvcInterceptorRegistrationAutoConfiguration` registers `FeatureFlagInterceptor` for all paths (`/**`).
2. `FeatureFlagInterceptor.preHandle()` checks `@FeatureFlag` on the method first, then on the class. Method-level annotation takes priority.
3. If the feature is disabled, `FeatureFlagAccessDeniedException` is thrown.
4. `FeatureFlagExceptionHandler` (`@ControllerAdvice`, `@Order(Ordered.LOWEST_PRECEDENCE)`) catches the exception and delegates to `AccessDeniedInterceptResolution.resolution()` to write the response.

### Extension Points

- **Custom feature source**: Implement `FeatureFlagProvider` (webmvc) or `ReactiveFeatureFlagProvider` (webflux) and register as a `@Bean`. The default `InMemoryFeatureFlagProvider` / `InMemoryReactiveFeatureFlagProvider` reads from `feature-flags.feature-names` in config and is **fail-closed by default** — feature names not present in the config are treated as disabled. Set `feature-flags.default-enabled: true` to switch to fail-open behavior. A custom bean replaces the default due to `@ConditionalOnMissingBean`.
- **Custom denied response**: Define a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.
- **Gradual rollout**: Use `@FeatureFlag(value = "name", rollout = 50)` to enable a feature for a percentage of requests. Implement `FeatureFlagContextResolver` (webmvc) or `ReactiveFeatureFlagContextResolver` (webflux) for sticky rollout. Implement `RolloutStrategy` (webmvc) or `ReactiveRolloutStrategy` (webflux) to customize bucketing.

### Auto-configuration Registration

All modules use Spring Boot's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to register their `@AutoConfiguration` classes. Auto-configuration ordering is enforced with `after =` / `beforeName =` references:
1. `FeatureFlagAutoConfiguration` (core)
2. `FeatureFlagActuatorAutoConfiguration` (actuator) — before webmvc/webflux
3. `FeatureFlagMvcAutoConfiguration` (webmvc)
4. `FeatureFlagMvcInterceptorRegistrationAutoConfiguration` (webmvc)
5. `FeatureFlagWebFluxAutoConfiguration` (webflux)

### Response Types

Built-in `AccessDeniedInterceptResolution` implementations (selected by `feature-flags.response.type`):
- `PLAIN_TEXT` → `AccessDeniedInterceptResolutionViaPlainTextResponse`
- `HTML` → `AccessDeniedInterceptResolutionViaHtmlResponse`
- `JSON` (default) → `AccessDeniedInterceptResolutionViaJsonResponse` (RFC 7807 / Problem Details format)

## Contributing

PRs target `main`. PR titles should be prefixed with `Close #<IssueNumber>` when resolving an issue. See `.github/CONTRIBUTING.md` for the full workflow.