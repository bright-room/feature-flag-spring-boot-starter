package net.brightroom.featureflag.response;

public enum Mode {
  JSON,
  TEXT;

  public boolean isText() {
    return this == TEXT;
  }
}
