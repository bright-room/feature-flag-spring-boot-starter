# Actuator Module

The `actuator` module provides a Spring Boot Actuator endpoint for reading and updating feature flags at runtime without restarting the application.

## Setup

1. Add the `actuator` dependency (see [Installation](../README.md#installation)).
2. Expose the endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: feature-flags
```

## Read all flags

```
GET /actuator/feature-flags
```

Response:

```json
{
  "features": {
    "hello-class": true,
    "user-find": false
  },
  "defaultEnabled": false
}
```

## Update a flag

```
POST /actuator/feature-flags
Content-Type: application/json

{
  "featureName": "user-find",
  "enabled": true
}
```

Response:

```json
{
  "features": {
    "hello-class": true,
    "user-find": true
  },
  "defaultEnabled": false
}
```

If the flag does not exist, it is created with the given state.

## Restricting access

By default, both read and write operations are unrestricted. In production, consider restricting access:

```yaml
management:
  endpoint:
    feature-flags:
      access: READ_ONLY
```

Or secure the endpoint with Spring Security.

## Event integration

A `FeatureFlagChangedEvent` is published every time a flag is updated via the actuator endpoint. Subscribe with `@EventListener` to react to changes (e.g., clearing caches, logging audit trails).

```java
@Component
class FeatureFlagChangeListener {

  @EventListener
  void onFlagChanged(FeatureFlagChangedEvent event) {
    log.info("Flag '{}' changed to {}", event.featureName(), event.enabled());
  }
}
```
