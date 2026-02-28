# コードレビュー: webflux モジュール

**レビュアー:** シニアデベロッパー (AI-assisted)
**日付:** 2026-03-01
**ブランチ:** `feature/add-flux-module`
**対象:** `webflux/` モジュール全体 (main / test / integrationTest)
**ベースコミット:** `bcde78e` (WebFilter を AOP+ControllerAdvice にリファクタリング)

---

## 目次

1. [総合評価](#1-総合評価)
2. [アーキテクチャレビュー](#2-アーキテクチャレビュー)
3. [プロダクションコード レビュー](#3-プロダクションコード-レビュー)
4. [テストコード レビュー](#4-テストコード-レビュー)
5. [ビルド・設定 レビュー](#5-ビルド設定-レビュー)
6. [指摘事項サマリ](#6-指摘事項サマリ)

---

## 1. 総合評価

webflux モジュールは、Spring WebFlux の2つのプログラミングモデル（アノテーションベースコントローラ / 関数型エンドポイント）の両方に対応した堅実な実装です。WebFilter から AOP+ControllerAdvice へのリファクタリング（コミット `bcde78e`）により、ハンドラの二重解決問題が解消され、webmvc モジュールとのエラーハンドリングアーキテクチャの整合性が確保されました。

**評価できる点:**

- AOP+ControllerAdvice アーキテクチャにより、webmvc の Interceptor+ControllerAdvice パターンとの概念的一貫性が確保されている
- `HtmlResponseBuilder` による2つの処理パス間での HTML レスポンス生成コードの重複排除
- `AccessDeniedHandlerFilterResolution` を `public` インターフェース化し `@ConditionalOnMissingBean` で制御することで、関数型エンドポイントのカスタマイズ性を確保
- `HtmlUtils.htmlEscape` による XSS 対策
- `Map.copyOf()` による不変コレクションを用いたスレッドセーフ設計
- 統合テストの網羅性（fail-closed / fail-open / 3種レスポンス形式 / カスタム例外ハンドラ / メソッドレベルオーバーライド）

**致命的な問題:**

P1-1 として報告する `Mono.block()` の問題は、**本番の Netty 環境で `IllegalStateException` を引き起こします**。テスト環境ではモックサーバーが使用されるため検出されません。マージ前に必ず対応が必要です。

---

## 2. アーキテクチャレビュー

### 2.1 2つの処理パス

webflux モジュールは、2つのエンドポイントスタイルに対してそれぞれ異なるメカニズムを提供しています。

| パス | 対象 | メカニズム | エラーハンドリング | カスタマイズ方法 |
|------|------|-----------|-------------------|-----------------|
| AOP Aspect | `@FeatureFlag` 付きコントローラ | `FeatureFlagAspect` (`@Around`) | `FeatureFlagExceptionHandler` (`@ControllerAdvice`) | ユーザー定義 `@ControllerAdvice`（webmvc と同一方式） |
| HandlerFilterFunction | `RouterFunction` 関数型エンドポイント | `FeatureFlagHandlerFilterFunction.of()` | `AccessDeniedHandlerFilterResolution`（直接レスポンス構築） | カスタム `AccessDeniedHandlerFilterResolution` Bean（`@ConditionalOnMissingBean`） |

**背景:** WebFilter ではなく AOP が採用された理由は、WebFilter 固有のハンドラ二重解決問題を回避するためです。WebFilter は `DispatcherHandler` の前段で動作するため、`@FeatureFlag` アノテーションを検査するには `handlerMapping.getHandler(exchange)` を別途呼び出す必要があり、`DispatcherHandler` 内部でも同じ解決が行われます。AOP はメソッド呼び出し時点で動作するため、メソッド/クラスのアノテーションに直接アクセスできます。

### 2.2 webmvc モジュールとの比較

| 観点 | webmvc | webflux |
|------|--------|---------|
| Auto-Configuration クラス数 | 2（`MvcAutoConfiguration` + `InterceptorRegistrationAutoConfiguration`） | 1（`WebFluxAutoConfiguration`） |
| リクエスト処理方式 | `HandlerInterceptor.preHandle()` | `@Aspect` + `@Around` |
| エラーハンドリング | `@ControllerAdvice` 例外ハンドラ | `@ControllerAdvice` 例外ハンドラ（同一パターン） |
| 関数型エンドポイント対応 | なし | `FeatureFlagHandlerFilterFunction` |
| Resolution インターフェースの可視性 | package-private | package-private（ExceptionHandler）/ **public**（HandlerFilter） |
| アノテーション解決順序 | メソッド → クラス（Interceptor 内） | メソッド → クラス（Aspect 内） |
| パスパターンサポート | `pathPatterns.includes/excludes`（`WebMvcConfigurer` 経由） | **未使用**（P2-5 参照） |

### 2.3 Auto-Configuration Bean グラフ

```
FeatureFlagWebFluxAutoConfiguration (after = FeatureFlagAutoConfiguration)
├── ReactiveFeatureFlagProvider          (@ConditionalOnMissingBean)
├── AccessDeniedExceptionHandlerResolution (Factory 経由)
├── FeatureFlagExceptionHandler          (ExceptionHandlerResolution に依存)
├── FeatureFlagAspect                    (ReactiveFeatureFlagProvider に依存)
├── AccessDeniedHandlerFilterResolution  (@ConditionalOnMissingBean)
└── FeatureFlagHandlerFilterFunction     (Provider + HandlerFilterResolution に依存)
```

---

## 3. プロダクションコード レビュー

### P1 (Critical)

---

#### P1-1 — `FeatureFlagAspect` が非リアクティブ戻り値型に対して `Mono.block()` を呼び出しており、本番 Netty 環境でクラッシュする

- [x] **修正済み**

**ファイル:** `webflux/src/main/java/net/brightroom/featureflag/webflux/configuration/FeatureFlagAspect.java`
**行:** 65-70

```java
// Non-reactive return type: block and check synchronously
boolean enabled = Boolean.TRUE.equals(enabledMono.block());  // 66行目
if (!enabled) {
  throw new FeatureFlagAccessDeniedException(featureName);
}
return joinPoint.proceed();
```

**問題の詳細:**

本番の Spring WebFlux アプリケーション（Netty 上で動作）では、コントローラメソッドの呼び出しは `reactor-http-nio-*` スレッド上で行われます。これらのスレッドは Reactor の `NonBlocking` マーカーインターフェースを実装しています。Reactor の `Mono.block()` 実装（`BlockingOptionalMonoSubscriber.blockingGet()` 内部）は `Schedulers.isInNonBlockingThread()` を呼び出し、`true` が返された場合に `IllegalStateException` をスローします。

```
java.lang.IllegalStateException: block()/blockFirst()/blockLast() are not
supported in thread reactor-http-nio-*
```

このチェックは **スレッドの種類** に基づいており、`Mono` が実際にブロッキングを必要とするかどうかには依存しません。`Mono.just(value).block()` であっても非ブロッキングスレッド上ではスローされます。

**テストが通過する理由:**

`@WebFluxTest` は **モックサーバー環境** を使用します（実際の Netty ではない）。`WebTestClient` が発行するリクエストはテストランナースレッド（例: `Test worker`, `main`）上で処理され、これらのスレッドは非ブロッキングスレッドとしてタグ付けされて**いません**。そのため `block()` はテスト環境では正常に動作しますが、**本番環境ではクラッシュします**。

影響を受ける全テストコントローラ（`String` 型＝非リアクティブ型を返却）:
- `FeatureFlagMethodLevelController.experimentalStageEndpoint()` (17行目)
- `FeatureFlagMethodLevelController.developmentStageEndpoint()` (23行目)
- `FeatureFlagDisableController.testDisable()` (12行目)
- `FeatureFlagEnableController.testEnabled()` (12行目)
- `FeatureFlagUndefinedFlagController.undefinedFlagEndpoint()` (19行目)

**影響:** `@FeatureFlag` が付与された全コントローラメソッドのうち、非リアクティブ型（`String`, `ResponseEntity`, POJO 等）を返すメソッドが、本番環境で `IllegalStateException` によりランタイムエラーとなります。

**修正案:**

**案A — 非リアクティブコードパスを除去する:**

WebFlux ライブラリであることを前提に、`@FeatureFlag` 付きメソッドは `Mono` または `Flux` を返すことを要件とし、非リアクティブ型の場合は明確なエラーをスローします。

```java
throw new IllegalStateException(
    "@FeatureFlag on WebFlux controller method '" + method.getName()
    + "' requires a reactive return type (Mono or Flux). "
    + "Non-reactive return types are not supported.");
```

**案B — 非リアクティブ戻り値型に対しても Aspect から `Mono` を返す:**

Spring WebFlux の `RequestMappingHandlerAdapter` は `HandlerMethodReturnValueHandler` を使用して実際の戻り値の型を処理します。Aspect が `Mono` を返した場合、リアクティブ戻り値ハンドラが正しく処理します。

```java
// 非リアクティブ戻り値型: Mono でラップしてリアクティブチェーンを維持
return enabledMono.flatMap(enabled -> {
    if (!enabled) {
        return Mono.error(new FeatureFlagAccessDeniedException(featureName));
    }
    try {
        return Mono.justOrEmpty(joinPoint.proceed());
    } catch (Throwable t) {
        return Mono.error(t);
    }
});
```

**注意:** 案B では、宣言上の戻り値型が非リアクティブであるメソッドから `Mono` を返した場合に Spring WebFlux が正しく処理することを、`@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` 統合テストで検証する必要があります。

**案C — ブロッキングを bounded elastic スレッドに移す:**

```java
boolean enabled = Boolean.TRUE.equals(
    enabledMono.subscribeOn(Schedulers.boundedElastic()).block());
```

この案はスレッドホップのオーバーヘッドが追加され、リアクティブライブラリの目的に反するため、最も望ましくない選択肢です。

---

### P2 (Medium)

---

#### P2-1 — `FeatureFlagHandlerFilterFunction.of()` で null パラメータ時に NPE が発生する

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/net/brightroom/featureflag/webflux/configuration/FeatureFlagHandlerFilterFunction.java`
**行:** 45-50

```java
public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    if (featureName.isEmpty()) {  // <-- featureName が null の場合 NPE
      throw new IllegalArgumentException(
          "featureName must not be empty. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
```

**背景:**

`of(String)` はユーザーが関数型エンドポイント設定時に直接呼び出す **公開 API** メソッドです。`@FeatureFlag` アノテーションの値は Java アノテーション仕様上 `null` になり得ませんが、通常のメソッドパラメータは `null` を受け取る可能性があります。特に設定ファイルや外部ソースから動的に値をロードする場合に発生し得ます。

**影響:** `null` が渡された場合、意味のあるメッセージを持たない `NullPointerException` がスローされ、デバッグが困難になります。

**推奨修正:**

```java
public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    if (featureName == null || featureName.isEmpty()) {
      throw new IllegalArgumentException(
          "featureName must not be null or empty. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
```

---

#### P2-2 — リアクティブコードパスがカスタム `ReactiveFeatureFlagProvider` からの空 `Mono` を処理しない

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/net/brightroom/featureflag/webflux/configuration/FeatureFlagAspect.java`
**行:** 38-48（Mono パス）, 52-62（Flux パス）

**ファイル:** `webflux/src/main/java/net/brightroom/featureflag/webflux/configuration/FeatureFlagHandlerFilterFunction.java`
**行:** 51-54

**問題の詳細:**

カスタム `ReactiveFeatureFlagProvider.isFeatureEnabled()` が `Mono.empty()`（要素なし）を返した場合、`flatMap`/`flatMapMany` のラムダが実行されません。結果として:

- **Aspect（Mono パス）:** 空の `Mono` が返却 → Spring WebFlux はボディなしの 200 を返す
- **Aspect（Flux パス）:** 空の `Flux` が返却 → Spring WebFlux はボディなしの 200 を返す
- **HandlerFilterFunction:** 空の `Mono<ServerResponse>` が返却 → フレームワーク依存の挙動（おそらくボディなし 200）

これらはいずれも正しい動作ではありません。本ライブラリの設計方針は **デフォルトで fail-closed** — 不明/未定義の状態ではアクセスを拒否すべきです。

**背景:** 組み込みの `InMemoryReactiveFeatureFlagProvider` は常に `Mono.just(...)` を返すため、この問題はカスタムプロバイダ実装でのみ顕在化します。ただし `ReactiveFeatureFlagProvider` は `public` な拡張ポイント（`@ConditionalOnMissingBean`）であり、インターフェース契約で `Mono.empty()` を明示的に禁止していません。

**非リアクティブパスとの比較:**

```java
// FeatureFlagAspect.java:66 — 非リアクティブパス（P1-1 の block() 問題あり）
boolean enabled = Boolean.TRUE.equals(enabledMono.block());
// block() は空 Mono に対して null を返す → Boolean.TRUE.equals(null) = false → アクセス拒否 ✓
```

非リアクティブパスは偶然にも `Mono.empty()` を正しく処理しています（fail-closed）。リアクティブパスも同様にすべきです。

**推奨修正:**

各リアクティブチェーンに `defaultIfEmpty(false)` を追加します。

```java
// FeatureFlagAspect.java — Mono パス
Mono<Boolean> enabledMono = reactiveFeatureFlagProvider.isFeatureEnabled(featureName)
    .defaultIfEmpty(false);

// FeatureFlagHandlerFilterFunction.java — of() メソッド
return (request, next) ->
    reactiveFeatureFlagProvider
        .isFeatureEnabled(featureName)
        .defaultIfEmpty(false)
        .flatMap(enabled -> filterByFeatureEnabled(request, next, featureName, enabled));
```

---

#### P2-3 — HTML / PlainText の Aspect 統合テストにメソッドレベルオーバーライドテストが欠落している

- [ ] **修正済み**

**ファイル:**
- `webflux/src/integrationTest/.../FeatureFlagAspectHtmlResponseIntegrationTest.java` (6テスト)
- `webflux/src/integrationTest/.../FeatureFlagAspectPlainTextResponseIntegrationTest.java` (6テスト)

**比較対象:**
- `webflux/src/integrationTest/.../FeatureFlagAspectJsonResponseIntegrationTest.java` (7テスト, 116-126行目):

```java
// JSON テストには存在するが、HTML/PlainText テストには存在しない:
@Test
void shouldAllowAccess_whenMethodAnnotationOverridesClassAnnotation() {
    webTestClient.get().uri("/test/method-override")
        .exchange().expectStatus().isOk()
        .expectBody(String.class).isEqualTo("Method Override Allowed");
}
```

**背景:** メソッドレベル `@FeatureFlag` がクラスレベル `@FeatureFlag` をオーバーライドする動作は、アノテーション解決ロジック（`FeatureFlagAspect.resolveAnnotation()`, 73-83行目）の中核機能です。この動作はレスポンス形式に依存しないため、全レスポンス形式で一貫して検証すべきです。テストコントローラ `FeatureFlagDisableController` は既に `/test/method-override` エンドポイント（16-20行目）を定義しており、クラスレベル `@FeatureFlag("disable-class-level-feature")`（無効）とメソッドレベル `@FeatureFlag("experimental-stage-endpoint")`（有効）を持っています。

**推奨修正:** `FeatureFlagAspectHtmlResponseIntegrationTest` と `FeatureFlagAspectPlainTextResponseIntegrationTest` の両方に `shouldAllowAccess_whenMethodAnnotationOverridesClassAnnotation` テストを追加してください。

---

#### P2-4 — カスタム `AccessDeniedHandlerFilterResolution` の Bean 差し替えを検証する統合テストが存在しない

- [ ] **修正済み**

**既存テスト:** `FeatureFlagCustomExceptionHandlerIntegrationTest.java` — AOP/ControllerAdvice パスにおいて、カスタム `@ControllerAdvice` がデフォルトの `FeatureFlagExceptionHandler` をオーバーライドすることを検証しています。

**不足テスト:** HandlerFilterFunction パスでカスタム `AccessDeniedHandlerFilterResolution` Bean を使用する同等のテストがありません。

**背景:** `FeatureFlagWebFluxAutoConfiguration` は `AccessDeniedHandlerFilterResolution` に `@ConditionalOnMissingBean`（42行目）を付与しており、Bean 差し替えをカスタマイズ手段として明示的にサポートしています。`AccessDeniedHandlerFilterResolution` インターフェースは `public`（28行目）であり、Javadoc にカスタム Bean の例（16-26行目）も記載されています。これはドキュメント化された公開拡張ポイントであり、統合テストで検証すべきです。

**推奨修正:** `FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest` を新規作成し、以下を検証します。
1. カスタム `AccessDeniedHandlerFilterResolution` Bean を定義する
2. デフォルトの Resolution が関数型エンドポイントで置き換えられることを検証する
3. カスタムレスポンス形式をアサートする

---

#### P2-5 — `feature-flags.path-patterns` 設定が webflux で暗黙的に無視される

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/net/brightroom/featureflag/webflux/configuration/FeatureFlagWebFluxAutoConfiguration.java`

**背景:** `core` モジュールの `FeatureFlagProperties` は `pathPatterns`（`includes` / `excludes`）プロパティを含んでいます。`webmvc` モジュールでは `FeatureFlagMvcInterceptorRegistrationAutoConfiguration` がこのプロパティを使用してインターセプタの適用パスを制御しています。`webflux` モジュールではこのプロパティは一切使用されていません。

**影響分析:**

AOP ベースのアプローチでは、影響は WebFilter 方式と比較して **低い** です。
- AOP Aspect は `@FeatureFlag` が付与されたメソッド/クラスにのみ発火するため、アノテーションなしのエンドポイントに対するパフォーマンスオーバーヘッドはありません
- パスパターンフィルタリングは、全リクエストに適用される WebFilter ではより重要でした

ただし、`webmvc` から `webflux` に移行するユーザーが `path-patterns` を設定し、動作することを期待する可能性があります。設定が暗黙的に無視されることは混乱を招きます。

**推奨修正案:**

1. **ドキュメントで差異を明記する:** README/CLAUDE.md に `path-patterns` は webflux モジュールには適用されない旨を記載する（AOP はアノテーション付きエンドポイントを直接ターゲットするため）
2. **起動時に警告ログを出力する:** `path-patterns` が設定されていて webflux モジュールがアクティブな場合、設定が無効である旨の警告を出力する
3. **Aspect 内にパスベースフィルタリングを実装する:** `FeatureFlagAspect.checkFeatureFlag()` 内でリクエストパスを `pathPatterns` と照合する。ただし AOP Aspect 内から `ServerHttpRequest` にアクセスするには追加の実装（`ReactiveRequestContextHolder`）が必要で、複雑性が増します

---

#### P2-6 — `FeatureFlagAspect` と `FeatureFlagHandlerFilterFunction` のユニットテストが存在しない

- [ ] **修正済み**

**現在のユニットテストカバレッジ:**
- `InMemoryReactiveFeatureFlagProviderTest` — 2テスト（fail-closed デフォルト動作、事前定義フィーチャー）
- `InMemoryReactiveFeatureFlagProviderDefaultEnabledTest` — 4テスト（fail-closed/open、明示的 enable/disable）

**不足しているユニットテスト:**

以下のエッジケースはユニットテストレベルでのみテスト可能であり、統合テストではカバーされていません。

| クラス | 未テストシナリオ | リスク |
|--------|-----------------|--------|
| `FeatureFlagAspect` | `@FeatureFlag("")`（空文字）→ `IllegalStateException` がスローされるべき | 未検証の場合、fail-open 脆弱性の可能性 |
| `FeatureFlagAspect` | 非リアクティブ戻り値型の挙動（P1-1） | 本番クラッシュ |
| `FeatureFlagAspect` | `resolveAnnotation()` でアノテーションが見つからない場合 → `null` を返して proceed するべき | サイレントパススルー障害 |
| `FeatureFlagHandlerFilterFunction` | `of(null)` → NPE（P2-1） | 不明確なエラーメッセージ |
| `FeatureFlagHandlerFilterFunction` | `of("")` → `IllegalArgumentException` がスローされるべき | エラーメッセージの正確性 |

**推奨修正:** `FeatureFlagAspectTest` と `FeatureFlagHandlerFilterFunctionTest` を作成し、上記のシナリオをカバーしてください。

---

### P3 (Low)

---

#### P3-1 — `MediaType` インスタンスが毎回再生成されている

- [ ] **修正済み**

**ファイル（4箇所）:**
- `webflux/.../AccessDeniedExceptionHandlerResolutionViaPlainTextResponse.java:17`
- `webflux/.../AccessDeniedExceptionHandlerResolutionViaHtmlResponse.java:19`
- `webflux/.../AccessDeniedHandlerFilterResolutionViaPlainTextResponse.java:22`
- `webflux/.../AccessDeniedHandlerFilterResolutionViaHtmlResponse.java:23`

```java
// AccessDeniedExceptionHandlerResolutionViaPlainTextResponse:17 の例:
.contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
```

**背景:** `MediaType` はイミュータブルであるため、`static final` 定数として安全に再利用できます。パフォーマンスへの影響は微小（軽量コンストラクタ、GC 圧力も無視可能）ですが、定数化は明確性と一貫性のための標準的な慣行です。

**補足:** JSON 実装では `MediaType.APPLICATION_PROBLEM_JSON`（既存の定数）を使用しており、こちらは最適化済みです。

**推奨修正:**

```java
private static final MediaType TEXT_PLAIN_UTF8 =
    new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
```

---

#### P3-2 — テストメソッド名の typo: `validatePredefinedFunctions` → `validatePredefinedFeatures`

- [ ] **修正済み**

**ファイル:** `webflux/src/test/java/.../provider/InMemoryReactiveFeatureFlagProviderTest.java`
**行:** 22

```java
@Test
void validatePredefinedFunctions() {  // "Functions" → "Features" が正しい
```

**背景:** テスト対象はフィーチャーフラグ（Feature）であり、関数（Function）ではありません。同じ typo が webmvc モジュールの `InMemoryFeatureFlagProviderTest` にも存在します。一貫性のため、両方を同時にリネームすることが理想的です。

---

#### P3-3 — テストコントローラ間でコンストラクタスタイルが不統一

- [ ] **修正済み**

**明示的 public コンストラクタあり:**
- `FeatureFlagMethodLevelController.java:27` — `public FeatureFlagMethodLevelController() {}`
- `FeatureFlagUndefinedFlagController.java:23` — `public FeatureFlagUndefinedFlagController() {}`

**明示的コンストラクタなし（コンパイラ生成のデフォルトコンストラクタ）:**
- `FeatureFlagDisableController.java`
- `FeatureFlagEnableController.java`
- `NoFeatureFlagController.java`

**背景:** `@WebFluxTest` ではどちらのスタイルでも正常に動作します。Java コンパイラは `public` クラスに対して `public` デフォルトコンストラクタを自動生成します。不統一は純粋にスタイル上の問題です。

**推奨修正:** 全テストコントローラでスタイルを統一してください（全て明示するか、全て省略するか）。

---

#### P3-4 — Auto-Configuration に `@ConditionalOnWebApplication(type = REACTIVE)` がない

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/.../configuration/FeatureFlagWebFluxAutoConfiguration.java`
**行:** 12

```java
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class FeatureFlagWebFluxAutoConfiguration {
```

**背景:** 標準的な Spring Boot スターターでは、Auto-Configuration クラスに `@ConditionalOnWebApplication(type = REACTIVE)` を付与して、リアクティブ環境でのみロードされることを保証します。このガードがない場合、webflux モジュールの JAR が Servlet ベース（webmvc）アプリケーションのクラスパスに誤って含まれた際、WebFlux 固有の型に対する `ClassNotFoundException` で Bean 生成が失敗します。

**補足:** webmvc モジュールの `FeatureFlagMvcAutoConfiguration` にも対応するガード条件（`@ConditionalOnWebApplication(type = SERVLET)`）がないため、プロジェクト内では一貫しています。ユーザーは意図的に `webmvc` または `webflux` を選択するため、リスクは低いです。

**推奨修正（防御的）:**

```java
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableAspectJAutoProxy(proxyTargetClass = true)
class FeatureFlagWebFluxAutoConfiguration {
```

---

#### P3-5 — `FeatureFlagHandlerFilterFunction` Bean に `@ConditionalOnMissingBean` がない

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/.../configuration/FeatureFlagWebFluxAutoConfiguration.java`
**行:** 47-53

```java
@Bean
FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
    ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
    AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution) {
  return new FeatureFlagHandlerFilterFunction(
      reactiveFeatureFlagProvider, accessDeniedHandlerResolution);
}
```

**背景:** `FeatureFlagHandlerFilterFunction` は `public class` です。ユーザーが同型のカスタム Bean を定義した場合（可能性は低い）、`NoUniqueBeanDefinitionException` がスローされます。主なカスタマイズポイントは `AccessDeniedHandlerFilterResolution`（`@ConditionalOnMissingBean` 付き）であり、`FeatureFlagHandlerFilterFunction` 自体の差し替えは稀なユースケースです。

**補足:** webmvc モジュールの `FeatureFlagInterceptor` Bean にも `@ConditionalOnMissingBean` がないため、両モジュール間で一貫しています。

**推奨修正:** 必要に応じて `@ConditionalOnMissingBean` を追加するか、`@AutoConfiguration` の `exclude` で制御する旨をドキュメントに記載してください。ユースケースの稀少性を考慮し、優先度は低いです。

---

#### P3-6 — `FeatureFlagAspect` Bean に `@ConditionalOnMissingBean` がない

- [ ] **修正済み**

**ファイル:** `webflux/src/main/java/.../configuration/FeatureFlagWebFluxAutoConfiguration.java`
**行:** 36-39

```java
@Bean
FeatureFlagAspect featureFlagAspect(ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    return new FeatureFlagAspect(reactiveFeatureFlagProvider);
}
```

**背景:** `FeatureFlagAspect` は **package-private** であるため、パッケージ外からユーザーが同型の代替 Bean を直接定義することはできません。ただし、サブクラスの作成や `@Primary` 経由での Bean 定義により競合が発生する可能性はあります。リスクは非常に低いです。

**補足:** webmvc モジュールの `FeatureFlagInterceptor` Bean にもこのアノテーションがないため、一貫しています。

---

## 4. テストコード レビュー

### 4.1 テストカバレッジマトリクス

#### Aspect（アノテーションベースコントローラ）

| テストシナリオ | JSON | HTML | PlainText | FailClosed | FailOpen | CustomHandler |
|---------------|:----:|:----:|:---------:|:----------:|:--------:|:-------------:|
| アノテーションなし → 許可 | x | x | x | | | |
| 有効フィーチャー → 許可 | x | x | x | | | |
| 無効フィーチャー → 403 | x | x | x | | | |
| `@FeatureFlag` なしコントローラ → 許可 | x | x | x | | | |
| クラスレベル無効 → 403 | x | x | x | | | |
| クラスレベル有効 → 許可 | x | x | x | | | |
| メソッドがクラスをオーバーライド → 許可 | x | | | | | |
| 未定義フラグ（fail-closed）→ 403 | | | | x | | |
| 未定義フラグ（fail-open）→ 許可 | | | | | x | |
| カスタム `@ControllerAdvice` → 503 | | | | | | x |

**ギャップ:** メソッドオーバーライドテストが HTML/PlainText で欠落（P2-3）

#### HandlerFilterFunction（関数型エンドポイント）

| テストシナリオ | JSON | HTML | PlainText | FailClosed | FailOpen |
|---------------|:----:|:----:|:---------:|:----------:|:--------:|
| フィルタなし → 許可 | x | x | x | | |
| 有効フィーチャー → 許可 | x | x | x | | |
| 無効フィーチャー → 403 | x | x | x | | |
| クラスレベル無効 → 403 | x | x | x | | |
| クラスレベル有効 → 許可 | x | x | x | | |
| 未定義フラグ（fail-closed）→ 403 | | | | x | |
| 未定義フラグ（fail-open）→ 許可 | | | | | x |

**ギャップ:** カスタム `AccessDeniedHandlerFilterResolution` テストが欠落（P2-4）

#### ユニットテスト

| クラス | テスト数 | カバレッジ |
|--------|:-------:|----------|
| `InMemoryReactiveFeatureFlagProviderTest` | 2 | fail-closed デフォルト、事前定義フィーチャー |
| `InMemoryReactiveFeatureFlagProviderDefaultEnabledTest` | 4 | fail-closed/open、明示的 enable/disable |
| `FeatureFlagAspect`（未作成） | 0 | — |
| `FeatureFlagHandlerFilterFunction`（未作成） | 0 | — |

### 4.2 テスト設定の分析

**ファイル:** `webflux/src/integrationTest/.../configuration/FeatureFlagWebFluxTestAutoConfiguration.java`

```java
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import(FeatureFlagWebFluxAutoConfiguration.class)
public class FeatureFlagWebFluxTestAutoConfiguration {}
```

**所見:** Javadoc（11-22行目）は、`@EnableAutoConfiguration` が既に `META-INF/spring/...AutoConfiguration.imports` 経由で `FeatureFlagWebFluxAutoConfiguration` をロードすること、および明示的な `@Import` は将来の Spring Boot スキャン順序変更に対するレジリエンスのために残していることを正しく説明しています。Spring は Bean 定義を重複排除するため、二重登録は無害です。これは十分にドキュメント化された意図的な設計です。

### 4.3 統合テストリソースファイル

**ファイル:** `webflux/src/integrationTest/resources/application.yaml`

```yaml
feature-flags:
  feature-names:
    experimental-stage-endpoint: true
    development-stage-endpoint: false
    enable-class-level-feature: true
    disable-class-level-feature: false
  response:
    type: json
```

**ファイル:** `webflux/src/test/resources/application.yaml`

```yaml
feature-flags:
  feature-names:
    experimental-stage-endpoint: true
    development-stage-endpoint: false
  response:
    type: json
```

**所見:** 統合テスト設定にはクラスレベルフィーチャーフラグ（`enable-class-level-feature`, `disable-class-level-feature`）が含まれていますが、ユニットテスト設定には含まれていません。これは正しいです — ユニットテストは `InMemoryReactiveFeatureFlagProvider` のみをテストしており、クラスレベルフィーチャーを必要としません。

---

## 5. ビルド・設定 レビュー

### 5.1 `build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(libs.spring.boot.starter.webflux)
    implementation("org.springframework.boot:spring-boot-starter-aspectj")  // 13行目

    testImplementation(libs.spring.boot.starter.webflux.test)

    integrationTestImplementation(libs.spring.boot.starter.webflux.test)
    integrationTestImplementation(libs.jsoup)
}
```

**所見:**

1. `spring-boot-starter-aspectj`（13行目）は、Auto-Configuration で `@EnableAspectJAutoProxy` を使用しているため、`implementation` として正しくインクルードされています。AOP ベースアプローチのハード依存です。

2. `jsoup` は `integrationTestImplementation` にのみスコープされており正しいです — 統合テストでの HTML レスポンスアサーションに使用されています。

3. **一貫性の指摘:** `spring-boot-starter-aspectj` はハードコードされた文字列（`"org.springframework.boot:spring-boot-starter-aspectj"`）を使用していますが、他の依存関係はバージョンカタログ参照（`libs.spring.boot.starter.webflux`）を使用しています。一貫性のため、バージョンカタログに追加することを検討してください。

### 5.2 Auto-Configuration 登録

**ファイル:** `webflux/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxAutoConfiguration
```

Auto-Configuration クラスは1つのみ登録されています。webmvc（2つ登録）より簡素化されています。この簡素化は以下の理由で適切です。
- WebFlux ではインターセプタ登録用の別 Auto-Configuration が不要（`WebMvcConfigurer.addInterceptors()` に相当するものが不要）
- AOP Aspect は Bean として登録されれば自動的に検出される

---

## 6. 指摘事項サマリ

### P1 (Critical)

- [x] **P1-1** — `FeatureFlagAspect` が非リアクティブ戻り値型で `Mono.block()` を呼び出し → 本番 Netty 環境で `IllegalStateException` — `FeatureFlagAspect.java:66`

### P2 (Medium)

- [x] **P2-1** — `FeatureFlagHandlerFilterFunction.of()` の null 未チェック → NPE — `FeatureFlagHandlerFilterFunction.java:46`
- [x] **P2-2** — リアクティブコードパスが空 `Mono` を未処理 → カスタムプロバイダで fail-open 化 — `FeatureFlagAspect.java:38,52` / `FeatureFlagHandlerFilterFunction.java:51`
- [x] **P2-3** — HTML / PlainText の Aspect テストにメソッドオーバーライドテストが欠落 — `FeatureFlagAspectHtmlResponseIntegrationTest.java` / `FeatureFlagAspectPlainTextResponseIntegrationTest.java`
- [x] **P2-4** — カスタム `AccessDeniedHandlerFilterResolution` の統合テストが未実装 — 新規ファイル作成が必要
- [x] **P2-5** — `feature-flags.path-patterns` が webflux で暗黙的に無視される — `FeatureFlagWebFluxAutoConfiguration.java`
- [x] **P2-6** — `FeatureFlagAspect` / `FeatureFlagHandlerFilterFunction` のユニットテストなし — 新規ファイル作成が必要

### P3 (Low)

- [ ] **P3-1** — `MediaType` インスタンスが毎回再生成 — 4ファイル
- [ ] **P3-2** — テストメソッド名の typo: `Functions` → `Features` — `InMemoryReactiveFeatureFlagProviderTest.java:22`
- [ ] **P3-3** — テストコントローラのコンストラクタスタイル不統一 — 5ファイル
- [ ] **P3-4** — Auto-Configuration に `@ConditionalOnWebApplication(type = REACTIVE)` なし — `FeatureFlagWebFluxAutoConfiguration.java:12`
- [ ] **P3-5** — `FeatureFlagHandlerFilterFunction` Bean に `@ConditionalOnMissingBean` なし — `FeatureFlagWebFluxAutoConfiguration.java:47`
- [ ] **P3-6** — `FeatureFlagAspect` Bean に `@ConditionalOnMissingBean` なし — `FeatureFlagWebFluxAutoConfiguration.java:36`

---

**P1: 1件 / P2: 6件 / P3: 6件 = 合計: 13件**
