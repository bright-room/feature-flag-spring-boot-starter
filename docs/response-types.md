# Response Types

When a feature flag is disabled, `FeatureFlagAccessDeniedException` is thrown and the response is returned with HTTP status `403 Forbidden`. The response format is selected by `feature-flags.response.type`.

## JSON Response (default)

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

## Plain Text Response

```yaml
feature-flags:
  response:
    type: PLAIN_TEXT
```

Response body:

```
Feature 'user-find' is not available
```

## HTML Response

```yaml
feature-flags:
  response:
    type: HTML
```

> **Note (Spring MVC only):** The HTML response is returned only when the client's `Accept` header includes `text/html` or `text/*`. If the client only accepts `application/json`, a `406 Not Acceptable` response is returned instead. In Spring WebFlux, the HTML response is always returned regardless of the `Accept` header.
