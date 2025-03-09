package net.brightroom.featureflag.response;

interface AccessDeniedResponse {
  Integer status();
  String body();
}
