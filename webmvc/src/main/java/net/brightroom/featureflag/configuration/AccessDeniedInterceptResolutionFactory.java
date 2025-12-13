package net.brightroom.featureflag.configuration;

import tools.jackson.databind.json.JsonMapper;

class AccessDeniedInterceptResolutionFactory {

  JsonMapper jsonMapper;
  boolean useRFC7807;

  AccessDeniedInterceptResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();
    ResponseType type = responseProperties.type();

    if (type == ResponseType.PLAIN_TEXT)
      return new AccessDeniedInterceptResolutionViaPlainTextResponse(
          responseProperties.statusCode(), responseProperties.message());

    if (type == ResponseType.VIEW) {
      ViewProperties viewProperties = responseProperties.view();
      return new AccessDeniedInterceptResolutionViaViewResponse(
          responseProperties.statusCode(), viewProperties.forwardTo(), viewProperties.attributes());
    }

    if (useRFC7807)
      return new AccessDeniedInterceptResolutionViaRFC7807JsonResponse(
          responseProperties.statusCode(), responseProperties.body(), jsonMapper);

    return new AccessDeniedInterceptResolutionViaJsonResponse(
        responseProperties.statusCode(), responseProperties.body(), jsonMapper);
  }

  AccessDeniedInterceptResolutionFactory(JsonMapper jsonMapper, boolean useRFC7807) {
    this.jsonMapper = jsonMapper;
    this.useRFC7807 = useRFC7807;
  }
}
