# WebFluxモジュール実装プラン

## 概要

Spring WebFlux向けの Feature Flag モジュール（`webflux`）を実装します。
webmvcモジュールと同様に `@FeatureFlag` アノテーションによるエンドポイントアクセス制御を提供しつつ、WebFlux特有のFunctional Endpointsもサポートします。

## 前提

- アノテーションベースとFunctional Endpointsは1つのモジュール内で共存可能（詳細は `questions.md` Q1参照）
- `FeatureFlagProvider` は `webflux` モジュールにリアクティブ版として新規配置する（webmvcの既存ユーザーに影響を与えない）
- webflux モジュールでは `ReactiveFeatureFlagProvider`（`Mono<Boolean>` を返す）を使用する

## 実装方針

| パターン | Feature Flagチェック | エラーハンドリング |
|----------|---------------------|-------------------|
| アノテーションベース | **`WebFilter`** で `RequestMappingHandlerMapping` からハンドラを解決し `@FeatureFlag` をチェック | `WebFilter` 内で `ServerHttpResponse` に直接403レスポンスを書き込み |
| Functional Endpoints | **`HandlerFilterFunction`** 内でチェック | `HandlerFilterFunction` 内で直接 `ServerResponse` を返却 |

WebFlux では `HandlerInterceptor` が存在せず、`WebFilter` は `DispatcherHandler` の外側で動作するため `@ControllerAdvice` は使えません（`questions.md` Q1参照）。
そのため、両パターンとも**フィルタ内で直接レスポンスを生成**する方式を採用します。
これは完全ノンブロッキングで動作し、WebFlux のイディオムに沿ったアプローチです。

```
アノテーションベースのリクエストフロー:

  Request → FilteringWebHandler → FeatureFlagWebFilter
    → RequestMappingHandlerMapping.getHandler() でハンドラ解決
    → HandlerMethod から @FeatureFlag を検出
      → Feature 有効: chain.filter(exchange) → 通常処理
      → Feature 無効: AccessDeniedReactiveResolution で 403 レスポンスを直接書き込み

Functional Endpoints のリクエストフロー:

  Request → DispatcherHandler → RouterFunctionMapping
    → HandlerFilterFunction.filter()
      → Feature 有効: next.handle(request) → HandlerFunction 実行
      → Feature 無効: ServerResponse.status(403) を直接返却
```

---

## Phase 1: ReactiveFeatureFlagProvider の実装

### 目的
WebFluxのノンブロッキングI/Oに適合するリアクティブな Feature Flag Provider を `webflux` モジュールに実装する。

### 新規作成

**`ReactiveFeatureFlagProvider`** (interface)
```java
public interface ReactiveFeatureFlagProvider {
    Mono<Boolean> isFeatureEnabled(String featureName);
}
```

**`InMemoryReactiveFeatureFlagProvider`** (default implementation)
```java
public class InMemoryReactiveFeatureFlagProvider implements ReactiveFeatureFlagProvider {
    private final Map<String, Boolean> features;
    private final boolean defaultEnabled;

    @Override
    public Mono<Boolean> isFeatureEnabled(String featureName) {
        return Mono.just(features.getOrDefault(featureName, defaultEnabled));
    }
}
```

- webmvc の `FeatureFlagProvider` / `InMemoryFeatureFlagProvider` と同等の機能
- `Mono<Boolean>` を返すことでノンブロッキング対応
- `@ConditionalOnMissingBean` でユーザーによるカスタム実装の差し替えが可能
- ユーザーが外部サービス（R2DBC、WebClient等）から Feature Flag を取得するカスタム実装を作成可能

### パッケージ
`net.brightroom.featureflag.webflux.provider`

---

## Phase 2: アノテーションベースのFeature Flagサポート（WebFilter）

### 2.1 WebFilter実装

**`FeatureFlagWebFilter`** (`implements WebFilter`)

`RequestMappingHandlerMapping` を使用してハンドラを解決し、`@FeatureFlag` アノテーションをチェックします。
全てリアクティブチェーン上で動作するため、完全ノンブロッキングです。

```java
class FeatureFlagWebFilter implements WebFilter {
    private final RequestMappingHandlerMapping handlerMapping;
    private final ReactiveFeatureFlagProvider provider;
    private final AccessDeniedReactiveResolution resolution;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return handlerMapping.getHandler(exchange)
            .flatMap(handler -> {
                if (!(handler instanceof HandlerMethod handlerMethod)) {
                    return chain.filter(exchange);
                }
                FeatureFlag annotation = resolveAnnotation(handlerMethod);
                if (annotation == null) {
                    return chain.filter(exchange);
                }
                return provider.isFeatureEnabled(annotation.value())
                    .flatMap(enabled -> {
                        if (enabled) {
                            return chain.filter(exchange);
                        }
                        return resolution.resolve(exchange,
                            new FeatureFlagAccessDeniedException(annotation.value()));
                    });
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    private FeatureFlag resolveAnnotation(HandlerMethod handlerMethod) {
        // メソッドレベル優先、なければクラスレベルをチェック
        FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
        if (methodAnnotation != null) return methodAnnotation;
        return handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    }
}
```

**設計ポイント:**
- `RequestMappingHandlerMapping.getHandler()` でハンドラを解決（リアクティブ）
- メソッドレベルアノテーション優先 → なければクラスレベルをチェック（webmvc と同じセマンティクス）
- `ReactiveFeatureFlagProvider.isFeatureEnabled()` の結果をリアクティブチェーンで処理（`.block()` 不要）
- Feature 無効時は `AccessDeniedReactiveResolution` で直接レスポンスを書き込み、フィルタチェーンを中断
- ハンドラが見つからない場合（Functional Endpoints 等）は `switchIfEmpty` でフィルタチェーンを続行

### 2.2 レスポンス生成

**`AccessDeniedReactiveResolution`** (interface)

```java
interface AccessDeniedReactiveResolution {
    Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e);
}
```

**実装クラス:**

| クラス | ResponseType | Content-Type | レスポンス形式 |
|--------|-------------|--------------|---------------|
| `AccessDeniedReactiveResolutionViaJsonResponse` | `JSON`（デフォルト） | `application/problem+json` | RFC 7807 Problem Details |
| `AccessDeniedReactiveResolutionViaPlainTextResponse` | `PLAIN_TEXT` | `text/plain; charset=UTF-8` | エラーメッセージ文字列 |
| `AccessDeniedReactiveResolutionViaHtmlResponse` | `HTML` | `text/html; charset=UTF-8` | HTML エラーページ |

各実装は `ServerHttpResponse` に直接書き込みます:
```java
class AccessDeniedReactiveResolutionViaJsonResponse implements AccessDeniedReactiveResolution {
    @Override
    public Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        // ProblemDetail JSON を DataBuffer に書き込み
        byte[] body = objectMapper.writeValueAsBytes(problemDetail);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
```

**`AccessDeniedReactiveResolutionFactory`**
- `FeatureFlagProperties.response().type()` に基づいて適切な実装を選択

---

## Phase 3: Functional Endpointsサポート

### 3.1 HandlerFilterFunction

**`FeatureFlagHandlerFilterFunction`**

Functional Endpointsの `RouterFunction` に適用するフィルタ関数を提供します。

**利用例:**
```java
@Bean
RouterFunction<ServerResponse> routes(FeatureFlagHandlerFilterFunction filterFunction) {
    return route()
        .GET("/api/feature", handler::handle)
        .filter(filterFunction.of("feature-name"))
        .build();
}
```

**処理フロー:**
```
1. ReactiveFeatureFlagProvider.isFeatureEnabled(featureName) をリアクティブにチェック
2. 有効 → next.handle(request) を実行（通常処理）
3. 無効 → 403 ServerResponse を直接返却（例外をthrowしない）
```

**レスポンス生成:**
- `ServerResponse.status(HttpStatus.FORBIDDEN)` でレスポンスを構築
- レスポンス形式は `FeatureFlagProperties.response().type()` に従う

### 3.2 ファクトリクラス

**`FeatureFlagHandlerFilterFunction`**

```java
public class FeatureFlagHandlerFilterFunction {
    // ファクトリメソッドで feature name を指定して HandlerFilterFunction を生成
    public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName);
}
```

- `ReactiveFeatureFlagProvider` とレスポンス設定を内部に保持
- `of()` メソッドで feature name を指定し、 `HandlerFilterFunction` を返却
- Spring Beanとして登録し、DIで注入可能にする

---

## Phase 4: Auto-Configuration

### 4.1 依存関係

```kotlin
// webflux/build.gradle.kts（既存のまま、AOP 依存は不要）
dependencies {
    api(projects.core)
    implementation(libs.spring.boot.starter.webflux)
    // ...
}
```

### 4.2 Auto-Configuration クラス

**`FeatureFlagWebFluxAutoConfiguration`**
- `@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)`
- Bean定義:
  - `ReactiveFeatureFlagProvider` — リアクティブFeature Flagプロバイダ（`@ConditionalOnMissingBean`）
  - `AccessDeniedReactiveResolution` — レスポンス生成戦略
  - `FeatureFlagWebFilter` — アノテーションベース用 WebFilter
  - `FeatureFlagHandlerFilterFunction` — Functional Endpoints用ユーティリティ

### 4.3 Auto-Configuration 登録

**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**
```
net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxAutoConfiguration
```

### 4.4 Auto-Configuration 読み込み順序

```
1. FeatureFlagAutoConfiguration (core)
     ↓
2. FeatureFlagWebFluxAutoConfiguration (webflux)
```

---

## Phase 5: テスト

### 5.1 ユニットテスト (`src/test`)

| テストクラス | 対象 |
|-------------|------|
| `InMemoryReactiveFeatureFlagProviderTest` | InMemoryReactiveFeatureFlagProviderのFeature Flagチェックロジック（fail-closed） |
| `InMemoryReactiveFeatureFlagProviderDefaultEnabledTest` | fail-open / fail-closed の両方の動作検証 |
| `FeatureFlagWebFilterTest` | WebFilterのアノテーション解決・Feature Flagチェックロジック |

### 5.2 インテグレーションテスト (`src/integrationTest`)

#### アノテーションベース

| テストクラス | テスト内容 |
|-------------|-----------|
| `FeatureFlagWebFilterJsonResponseIntegrationTest` | JSON（RFC 7807）レスポンスの検証 |
| `FeatureFlagWebFilterPlainTextResponseIntegrationTest` | プレーンテキストレスポンスの検証 |
| `FeatureFlagWebFilterHtmlResponseIntegrationTest` | HTMLレスポンスの検証 |
| `FeatureFlagWebFilterFailClosedIntegrationTest` | Fail-closed動作の検証 |
| `FeatureFlagWebFilterFailOpenIntegrationTest` | Fail-open動作の検証 |
| `FeatureFlagWebFilterCustomResolutionIntegrationTest` | カスタム `AccessDeniedReactiveResolution` Bean 差し替えの検証 |

#### Functional Endpoints

| テストクラス | テスト内容 |
|-------------|-----------|
| `FeatureFlagHandlerFilterFunctionJsonResponseIntegrationTest` | JSON レスポンスの検証 |
| `FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest` | プレーンテキストレスポンスの検証 |
| `FeatureFlagHandlerFilterFunctionHtmlResponseIntegrationTest` | HTML レスポンスの検証 |
| `FeatureFlagHandlerFilterFunctionFailClosedIntegrationTest` | Fail-closed動作の検証 |
| `FeatureFlagHandlerFilterFunctionFailOpenIntegrationTest` | Fail-open動作の検証 |

#### テストインフラ

| ファイル | 役割 |
|---------|------|
| `TestApplication.java` | テスト用SpringBootアプリケーション |
| `FeatureFlagWebFluxTestAutoConfiguration.java` | テスト用Auto-Configuration |
| テスト用Controller群 | アノテーションベーステスト用エンドポイント |
| テスト用RouterFunction群 | Functional Endpointsテスト用ルーティング |

---

## モジュール構成（ファイルツリー）

```
webflux/
├── build.gradle.kts (既存)
├── src/
│   ├── main/
│   │   ├── java/net/brightroom/featureflag/webflux/
│   │   │   ├── provider/
│   │   │   │   ├── ReactiveFeatureFlagProvider.java
│   │   │   │   └── InMemoryReactiveFeatureFlagProvider.java
│   │   │   └── configuration/
│   │   │       ├── FeatureFlagWebFluxAutoConfiguration.java
│   │   │       ├── FeatureFlagWebFilter.java
│   │   │       ├── FeatureFlagHandlerFilterFunction.java
│   │   │       ├── AccessDeniedReactiveResolution.java
│   │   │       ├── AccessDeniedReactiveResolutionFactory.java
│   │   │       ├── AccessDeniedReactiveResolutionViaJsonResponse.java
│   │   │       ├── AccessDeniedReactiveResolutionViaPlainTextResponse.java
│   │   │       └── AccessDeniedReactiveResolutionViaHtmlResponse.java
│   │   └── resources/
│   │       └── META-INF/spring/
│   │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   ├── test/
│   │   └── java/net/brightroom/featureflag/webflux/
│   │       ├── provider/
│   │       │   ├── InMemoryReactiveFeatureFlagProviderTest.java
│   │       │   └── InMemoryReactiveFeatureFlagProviderDefaultEnabledTest.java
│   │       └── configuration/
│   │           └── FeatureFlagWebFilterTest.java
│   └── integrationTest/
│       └── java/net/brightroom/featureflag/webflux/
│           ├── TestApplication.java
│           ├── configuration/
│           │   └── FeatureFlagWebFluxTestAutoConfiguration.java
│           ├── endpoint/
│           │   ├── FeatureFlagDisableController.java
│           │   ├── FeatureFlagEnableController.java
│           │   ├── FeatureFlagMethodLevelController.java
│           │   ├── NoFeatureFlagController.java
│           │   ├── FeatureFlagUndefinedFlagController.java
│           │   └── FeatureFlagRouterConfiguration.java
│           ├── FeatureFlagWebFilterJsonResponseIntegrationTest.java
│           ├── FeatureFlagWebFilterPlainTextResponseIntegrationTest.java
│           ├── FeatureFlagWebFilterHtmlResponseIntegrationTest.java
│           ├── FeatureFlagWebFilterFailClosedIntegrationTest.java
│           ├── FeatureFlagWebFilterFailOpenIntegrationTest.java
│           ├── FeatureFlagWebFilterCustomResolutionIntegrationTest.java
│           ├── FeatureFlagHandlerFilterFunctionJsonResponseIntegrationTest.java
│           ├── FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest.java
│           ├── FeatureFlagHandlerFilterFunctionHtmlResponseIntegrationTest.java
│           ├── FeatureFlagHandlerFilterFunctionFailClosedIntegrationTest.java
│           └── FeatureFlagHandlerFilterFunctionFailOpenIntegrationTest.java
```

---

## webmvc モジュールとの対応関係

| 関心事 | webmvc | webflux |
|--------|--------|---------|
| Feature Flagチェック | `FeatureFlagInterceptor` (`HandlerInterceptor`) | `FeatureFlagWebFilter` (`WebFilter`) |
| エラーハンドリング | `FeatureFlagExceptionHandler` (`@ControllerAdvice`) | `WebFilter` / `HandlerFilterFunction` 内で直接レスポンス生成 |
| レスポンス生成 | `AccessDeniedInterceptResolution` → `ResponseEntity<?>` | `AccessDeniedReactiveResolution` → `Mono<Void>`（`ServerHttpResponse` に直接書き込み） |
| Feature Flag Provider | `FeatureFlagProvider` (sync `boolean`) | `ReactiveFeatureFlagProvider` (async `Mono<Boolean>`) |
| Functional Endpoints | — | `FeatureFlagHandlerFilterFunction` |

---

## カスタマイズポイント（ユーザー向け）

| カスタマイズ | 方法 |
|-------------|------|
| Feature Flag ソースの変更 | `ReactiveFeatureFlagProvider` を実装し `@Bean` として登録（R2DBC、WebClient等によるノンブロッキング取得が可能） |
| アクセス拒否レスポンスの変更 | `AccessDeniedReactiveResolution` を実装し `@Bean` として登録 |

---

## 実装順序

1. **Phase 1**: ReactiveFeatureFlagProvider の実装（リアクティブプロバイダ + InMemory実装）
2. **Phase 2**: アノテーションベースサポート（WebFilter + レスポンス生成）
3. **Phase 3**: Functional Endpointsサポート（HandlerFilterFunction）
4. **Phase 4**: Auto-Configuration 構築
5. **Phase 5**: テスト実装
