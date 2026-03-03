# Custom Feature Flag Provider

By default, feature flags are managed in the configuration file, but it is also possible to change the source destination.

By changing the source of feature flag management to a database, external file, etc., it is possible to control in real time.

To change the source destination, simply implement the `FeatureFlagProvider` and register the bean.

```java

// FeatureFlagExternalDataSourceProvider.java
@Component
class FeatureFlagExternalDataSourceProvider implements FeatureFlagProvider {

  FeatureManagementMapper featureManagementMapper;

  @Override
  public boolean isFeatureEnabled(String featureName) {
    Boolean enabled = featureManagementMapper.check(featureName);
    // Choose your undefined-flag policy:
    //   return false; — fail-closed: block access for undefined flags (recommended)
    //   return true; — fail-open: allow access for undefined flags
    if (enabled == null) return false;
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
