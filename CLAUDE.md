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

This is a multi-module Gradle project (Java 25, Spring Boot 4.x) that provides feature flag support for Spring MVC applications. It is published to Maven Central under group `net.bright-room.feature-flag-spring-boot-starter`.

### Modules

- **`core`** — Annotation and configuration properties. Contains `@FeatureFlag` annotation and `FeatureFlagProperties` (`feature-flags.*` config prefix). `FeatureFlagAutoConfiguration` bootstraps property binding.
- **`webmvc`** — Spring MVC interceptor implementation. Depends on `core`.
- **`gradle-scripts`** — Composite build providing convention plugins: `spring-boot-starter`, `publish-plugin`, `spotless-java`, `spotless-kotlin`.

### Request Flow

1. `FeatureFlagMvcInterceptorRegistrationAutoConfiguration` registers `FeatureFlagInterceptor` for all paths (`/**`).
2. `FeatureFlagInterceptor.preHandle()` checks `@FeatureFlag` on the method first, then on the class. Method-level annotation takes priority.
3. If the feature is disabled, `FeatureFlagAccessDeniedException` is thrown.
4. `FeatureFlagExceptionHandler` (`@ControllerAdvice`, `@Order(Ordered.LOWEST_PRECEDENCE)`) catches the exception and delegates to `AccessDeniedInterceptResolution.resolution()` to write the response.

### Extension Points

- **Custom feature source**: Implement `FeatureFlagProvider` and register as a `@Bean`. The default `InMemoryFeatureFlagProvider` reads from `feature-flags.feature-names` in config and is **fail-closed by default** — feature names not present in the config are treated as disabled. Set `feature-flags.default-enabled: true` to switch to fail-open behavior. A custom bean replaces the default due to `@ConditionalOnMissingBean`.
- **Custom denied response**: Define a `@ControllerAdvice` that handles `FeatureFlagAccessDeniedException`. It takes priority over the library's default handler.

### Auto-configuration Registration

Both modules use Spring Boot's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to register their `@AutoConfiguration` classes. Auto-configuration ordering is enforced with `after =` references:
1. `FeatureFlagAutoConfiguration` (core)
2. `FeatureFlagMvcAutoConfiguration` (webmvc)
3. `FeatureFlagMvcInterceptorRegistrationAutoConfiguration` (webmvc)

### Response Types

Built-in `AccessDeniedInterceptResolution` implementations (selected by `feature-flags.response.type`):
- `PLAIN_TEXT` → `AccessDeniedInterceptResolutionViaPlainTextResponse`
- `HTML` → `AccessDeniedInterceptResolutionViaHtmlResponse`
- `JSON` (default) → `AccessDeniedInterceptResolutionViaJsonResponse` (RFC 7807 / Problem Details format)

## Contributing

PRs target `main`. PR titles should be prefixed with `Close #<IssueNumber>` when resolving an issue. See `.github/CONTRIBUTING.md` for the full workflow.