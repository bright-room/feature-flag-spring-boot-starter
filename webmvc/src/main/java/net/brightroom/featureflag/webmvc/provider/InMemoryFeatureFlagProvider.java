package net.brightroom.featureflag.webmvc.provider;

import java.util.Map;

/**
 * @deprecated Use {@link net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider}
 *     instead. This alias will be removed in the next major version.
 */
@Deprecated(forRemoval = true)
public class InMemoryFeatureFlagProvider
    extends net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider {

  public InMemoryFeatureFlagProvider(Map<String, Boolean> features, boolean defaultEnabled) {
    super(features, defaultEnabled);
  }
}
