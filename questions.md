# WebFluxモジュール実装に関するQ&A

## Q1: アノテーションベースとFunctionalEndpointは1つのモジュールで共存できるか？

**回答: 可能です。**

Spring WebFluxでは、`DispatcherHandler`が以下の2つのHandlerMappingを同時に管理しています:

- `RequestMappingHandlerMapping` — アノテーションベース（`@Controller` / `@RestController`）
- `RouterFunctionMapping` — Functional Endpoints（`RouterFunction` / `HandlerFunction`）

両者は独立して動作するため、1つのモジュール内で両方のパターンをサポートできます。

### 各パターンの実装方針

| パターン | Feature Flag適用方法 | エラーハンドリング |
|----------|---------------------|-------------------|
| アノテーションベース | `WebFilter` でハンドラを解決し `@FeatureFlag` をチェック | `WebFilter` 内で直接レスポンスを書き込み（403） |
| Functional Endpoints | `HandlerFilterFunction` で Feature Flag をチェック | `HandlerFilterFunction` 内で `ServerResponse` を返却 |

### 追加Q&A: WebFilterの `Mono.error()` と `@ControllerAdvice` / `WebExceptionHandler` の関係

#### Q: WebFilterで `Mono.error(e)` をスローすれば `@ControllerAdvice` や `WebExceptionHandler` まで到達するか？

**`WebExceptionHandler` → 到達する（YES）**
**`@ControllerAdvice` → 到達しない（NO）** — Springチームが「by design」と明言

Spring WebFluxのリクエスト処理パイプラインは以下の階層構造になっています:

```
HttpWebHandlerAdapter
  └─ ExceptionHandlingWebHandler  ← WebExceptionHandler がここでエラーをキャッチ
       └─ FilteringWebHandler     ← WebFilter チェーンはここで実行
            └─ DispatcherHandler
                 └─ HandlerAdapter ← @ControllerAdvice はここの内部でのみ有効
```

- `WebFilter` は `DispatcherHandler` より **外側** で動作する
- `@ControllerAdvice` は `DispatcherHandler` の **内側**（`HandlerAdapter` 経由のコントローラメソッド実行時）でのみ例外をキャッチする
- したがって `WebFilter` で `Mono.error()` を返しても `@ControllerAdvice` には到達しない

> **参考:** [Spring Framework Issue #32924](https://github.com/spring-projects/spring-framework/issues/32924) にて Spring チームの @rstoyanchev が以下のように回答:
> *"Indeed this is by design. WebFilters are ahead of the DispatcherHandler and outside of higher level, web framework handling through annotated controller methods, and controller advice."*

#### Q: 他の選択肢として AOP は使えるか？

**使えます。** AOP（`@Aspect`）はアノテーションベースのコントローラに対して有力な代替手段です。

**AOP の仕組み:**

```java
@Aspect
public class FeatureFlagAspect {
    @Around("@annotation(featureFlag)")  // メソッドレベル
    public Object checkMethodLevel(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) {
        if (!provider.isFeatureEnabled(featureFlag.value())) {
            throw new FeatureFlagAccessDeniedException(featureFlag.value());
        }
        return joinPoint.proceed();
    }

    @Around("@within(featureFlag)")  // クラスレベル
    public Object checkClassLevel(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) {
        // 同様のチェック
    }
}
```

**AOP の利点:**
- AOP はコントローラの **内側**（`HandlerAdapter` 経由）で動作するため、例外が `@ControllerAdvice` に**到達する**
- webmvcと同じ `@ControllerAdvice` + `@ExceptionHandler` パターンが使える
- `RequestMappingHandlerMapping` を手動で解決する必要がない
- `@annotation` でメソッドレベル、`@within` でクラスレベルのアノテーションを検出可能
- リアクティブ戻り値（`Mono`/`Flux`）とも問題なく動作（`proceed()` 前に同期的に例外をthrowするため）

**AOP の注意点:**
- `spring-boot-starter-aop` への依存が必要
- Functional Endpoints には適用不可（アノテーションが存在しないため）

#### WebFilter vs AOP の比較

| 観点 | WebFilter | AOP (`@Aspect`) |
|------|-----------|-----------------|
| `@ControllerAdvice` で例外キャッチ | **不可**（by design） | **可能** |
| `WebExceptionHandler` で例外キャッチ | **可能** | 直接は不可（ControllerAdvice が先に処理） |
| 動作レイヤー | `DispatcherHandler` の外側 | `DispatcherHandler` の内側 |
| メソッド/クラスレベルアノテーション検出 | `HandlerMapping` の手動解決が必要 | `@annotation` / `@within` ポイントカットで自然に検出 |
| 追加依存 | なし | `spring-boot-starter-aop` |
| Functional Endpoints への適用 | 不可（別途 `HandlerFilterFunction` が必要） | 不可（別途 `HandlerFilterFunction` が必要） |
| webmvc との設計パターン一致度 | 低い（例外ハンドリングが異なる） | **高い**（同じ ControllerAdvice パターン） |

→ **アノテーションベースに対してはAOPの方がwebmvcとの一貫性が高く、よりシンプルに実装できる可能性があります。どちらを採用するか決定してください。**

#### Q: 1モジュールで両パターンをサポートする最善の組み合わせは？

まず用語の整理として、**Feature Flagチェック機構**と**エラーハンドリング機構**は別の関心事です:

| 関心事 | アノテーションベース | Functional Endpoints |
|--------|---------------------|---------------------|
| **Feature Flagチェック** | AOP（`@Aspect`）or WebFilter | `HandlerFilterFunction` |
| **エラーハンドリング** | `@ControllerAdvice`（AOP使用時）or `WebExceptionHandler`（WebFilter使用時） | `HandlerFilterFunction` 内で直接 `ServerResponse` を返却（例外不要） |

**推奨の組み合わせ:**

```
┌─────────────────────────────────────────────────────────────┐
│ webflux モジュール                                           │
│                                                             │
│  アノテーションベース:                                        │
│    Feature Flagチェック → AOP (@Aspect)                      │
│    エラーハンドリング   → @ControllerAdvice + @ExceptionHandler│
│                          (webmvc と同じパターン)              │
│                                                             │
│  Functional Endpoints:                                      │
│    Feature Flagチェック → HandlerFilterFunction               │
│    エラーハンドリング   → HandlerFilterFunction 内で           │
│                          ServerResponse を直接返却            │
│                          (例外をthrowする必要がない)           │
└─────────────────────────────────────────────────────────────┘
```

**ポイント:**
- アノテーションベースではAOP経由で例外がthrowされるため、`@ControllerAdvice` が自然に動作する（webmvcと同一パターン）
- Functional Endpointsでは `HandlerFilterFunction` が Feature Flagチェック + エラーレスポンス生成を一体で行うため、`WebExceptionHandler` は不要
- `WebFilter` も `WebExceptionHandler` もこの構成では不要（AOP + HandlerFilterFunction で完結）

#### Q: AOPを使う場合、利用者側のアプリに追加の依存関係が必要か？

**不要です。** webfluxモジュール自体が `spring-boot-starter-aop` を依存関係に宣言するため、利用者のアプリには**推移的依存**（transitive dependency）として自動的に含まれます。

```kotlin
// webflux/build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
```

利用者は従来通り webflux モジュールを追加するだけで OK:
```kotlin
// 利用者のbuild.gradle.kts
dependencies {
    implementation("net.bright-room.feature-flag-spring-boot-starter:webflux:3.0.0")
    // spring-boot-starter-aop は推移的に含まれる → 明示的な追加不要
}
```

`spring-boot-starter-aop` の中身は Spring AOP + AspectJ weaver で、Spring Boot エコシステムでは一般的な依存です（`spring-boot-starter-data-jpa` 等も内部で使用）。`AopAutoConfiguration` により自動設定されるため、利用者側での追加設定も不要です。

#### Q: @ControllerAdvice を実現する AOP 以外のアプローチはあるか？

AOP 以外で `@ControllerAdvice` に例外を到達させる方法を検討しましたが、いずれも AOP より劣ります:

| アプローチ | 概要 | 評価 |
|-----------|------|------|
| **カスタム `RequestMappingHandlerAdapter`** | ハンドラ実行前にアノテーションチェックを挿入 | 侵襲的。Spring内部APIへの依存度が高く、バージョンアップで壊れるリスク |
| **`@ModelAttribute` in `@ControllerAdvice`** | 全リクエストで `@ModelAttribute` メソッドを実行し、exchange属性からハンドラを取得してチェック | フラジャイル。HandlerMethodの取得がexchange属性経由で不安定 |
| **カスタム `HandlerMapping`** | ハンドラ解決時にアノテーションをチェック | 複雑。既存のHandlerMappingとの統合が困難 |
| **AOP (`@Aspect`)** | ポイントカットで `@FeatureFlag` を検出、`proceed()` 前に例外throw | **最善。** クリーンで標準的。Spring AOPの正規のユースケース |

**結論: `@ControllerAdvice` を使うなら AOP が最善であり、唯一の実用的な選択肢です。**

代替として「`@ControllerAdvice` を使わない」アプローチもあります:

- **WebFilter + `WebExceptionHandler`**: WebFlux固有のイディオム。`@ControllerAdvice` は使えないが、`WebExceptionHandler` で同等のカスタマイズが可能
- メリット: 追加依存なし、WebFluxのネイティブなパターン
- デメリット: webmvcとの設計一貫性が失われる

#### Q: webflux が AOP を使うなら、webmvc 側も AOP に揃えた方がいいか？

**揃える必要はありません。** 各モジュールはフレームワークのイディオムに従うべきです。

```
webmvc:  HandlerInterceptor → 例外throw → @ControllerAdvice でキャッチ
webflux: AOP (@Aspect)      → 例外throw → @ControllerAdvice でキャッチ
         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
         ↑ インターセプト手段は異なるが、エラーハンドリングは同じパターン
```

| 観点 | webmvc: HandlerInterceptor | webmvc: AOP（仮に変更した場合） |
|------|----------------------------|-------------------------------|
| フレームワークとの親和性 | **MVC の正規メカニズム** | MVC では非標準 |
| path pattern フィルタリング | `addPathPatterns` / `excludePathPatterns` で **ネイティブサポート** | AOP では不可（全 `@FeatureFlag` メソッドが対象） |
| 既存ユーザーへの影響 | なし（現行のまま） | **破壊的変更**（path pattern 設定が無効化） |
| `@ControllerAdvice` 対応 | **対応済み**（Interceptor の例外は ControllerAdvice に到達する） | 対応 |

ポイント:
- webmvc の `HandlerInterceptor` は `DispatcherServlet` の **内側** で動作するため、例外は `@ControllerAdvice` に到達する（WebFlux の WebFilter とは異なる）
- つまり webmvc は現行のままで `@ControllerAdvice` パターンが成立している
- webflux で AOP を採用する理由は「WebFlux に `HandlerInterceptor` が存在しない」からであり、webmvc には当てはまらない

**結論:** インターセプト手段が異なっても、エラーハンドリングの `@ControllerAdvice` + `@ExceptionHandler` パターンは両モジュールで共通になるため、ユーザーから見た一貫性は保たれます。

#### Q: AOP を使うデメリットは？

| デメリット | 詳細 | 影響度 |
|-----------|------|--------|
| **プロキシベースの制約** | Spring AOP は CGLIB プロキシで動作するため、**自己呼び出し**（同一クラス内のメソッド呼び出し）ではアスペクトが発火しない。`final` クラスや `private` メソッドも対象外 | 中。ただし通常のコントローラ利用ではほぼ問題にならない |
| **メソッド/クラスレベルの優先度制御** | `@annotation` と `@within` の両方のポイントカットがマッチした場合、両方のアドバイスが実行される。webmvc と同じ「メソッドレベル優先」セマンティクスを実現するには、クラスレベルアドバイス内でメソッドレベルアノテーションの有無をチェックしスキップするロジックが必要 | 中。実装上の考慮が必要 |
| **追加の依存関係** | `spring-boot-starter-aop`（AspectJ weaver + Spring AOP）が推移的に利用者のクラスパスに追加される | 低。一般的なライブラリであり、多くの Spring Boot アプリで既に使用されている |
| **暗黙的な動作** | `@FeatureFlag` アノテーションが AOP で処理されることがコード上からは見えにくい。デバッグ時のスタックトレースにプロキシ/AOP フレームが含まれる | 低。ドキュメントで明示すれば十分 |
| **path pattern 非対応** | AOP はポイントカットでマッチするため、webmvc のような `include` / `exclude` パスパターン設定ができない | 低。`@FeatureFlag` アノテーション自体がオプトイン方式なので、パスパターンの必要性は低い |

**特に注意すべきは「メソッド/クラスレベルの優先度制御」** です。webmvc では `HandlerInterceptor` 内で明示的にメソッド → クラスの順でチェックしていますが、AOP では両アドバイスが発火するためクラスレベル側で制御が必要です:

```java
@Around("@within(featureFlag)")
public Object checkClassLevel(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) {
    // メソッドレベルの @FeatureFlag が存在する場合はスキップ（メソッドレベル優先）
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    if (method.isAnnotationPresent(FeatureFlag.class)) {
        return joinPoint.proceed(); // メソッドレベルアドバイスに委ねる
    }
    // クラスレベルのチェックを実行
    if (!provider.isFeatureEnabled(featureFlag.value())) {
        throw new FeatureFlagAccessDeniedException(featureFlag.value());
    }
    return joinPoint.proceed();
}
```

#### Q: WebFilter を使った場合、ユーザーのカスタマイズのしやすさは損なわれるか？

**カスタマイズ自体は可能ですが、メカニズムが異なります。**

| カスタマイズ内容 | AOP 方式 | WebFilter 方式 |
|-----------------|----------|---------------|
| **レスポンス内容の変更** | `AccessDeniedReactiveResolution` Bean を差し替え | 同じ |
| **エラーハンドリングの上書き** | `@ControllerAdvice` + `@ExceptionHandler` で `FeatureFlagAccessDeniedException` をハンドル（**webmvc と同じ API**） | `WebExceptionHandler` を `@Order(-2)` 等で登録（**WebFlux 固有の API**） |
| **Feature Flag ソースの変更** | `ReactiveFeatureFlagProvider` Bean を差し替え | 同じ |
| **学習コスト** | webmvc 利用者は既知のパターン | `WebExceptionHandler` は webmvc にはない概念。新たに学習が必要 |

**WebFilter 方式でのエラーハンドリングカスタマイズ例:**

```java
// WebExceptionHandler によるカスタムエラーハンドリング
@Component
@Order(-2) // Spring Boot のデフォルトハンドラより前に実行
public class CustomFeatureFlagExceptionHandler implements WebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof FeatureFlagAccessDeniedException e) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            // カスタムレスポンスを生成...
            return exchange.getResponse().writeWith(...);
        }
        return Mono.error(ex); // 他の例外は次のハンドラへ
    }
}
```

```java
// 比較: AOP 方式の場合（webmvc と同じ書き方）
@ControllerAdvice
@Order(0)
public class CustomFeatureFlagExceptionHandler {
    @ExceptionHandler(FeatureFlagAccessDeniedException.class)
    public ResponseEntity<?> handle(FeatureFlagAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(...);
    }
}
```

**まとめ:**

- カスタマイズの **できること** は両方式でほぼ同等
- 違いは **やり方**（API）— AOP なら `@ControllerAdvice`、WebFilter なら `WebExceptionHandler`
- webmvc → webflux の移行を想定する利用者にとっては、AOP 方式の方が学習コストが低い
