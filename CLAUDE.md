# CLAUDE.md

Web上で動くTODOアプリ (webtodo)。ユーザー登録/ログイン、TODOの追加・編集・削除・完了切替、期限日を提供する。

## 役割分担(最重要)

- **ソースコード・Flywayマイグレーション・テンプレートはユーザー自身が書く。**
- Claudeはコードを直接作成・編集しない。方針、設計、実装手順、レビュー観点を示す役に徹する。
- コード例は要点を示す最小限のスニペットに留める。ファイル全体を提示しない。
- ユーザーが明示的に依頼した場合のみファイルを編集する。
- ユーザーが書いたコードのレビュー時は、バグ・セキュリティ・この文書の規約違反を指摘する。

## 技術スタック

- Java 21 / Spring Boot 4.0.8 / Gradle (Kotlin DSL)
- Spring MVC + Thymeleaf(サーバーサイドレンダリング) + htmx(部分更新)。SPA・独自JSフレームワークは使わない
- CSS: Pico.css(クラスレス寄り。WebJarsで配布物をGradle管理し、CDNは使わない。JS・ビルド工程は導入しない)
- Spring Data JPA + PostgreSQL + Flyway
- Spring Security(フォームログイン)
- Lombok
- テスト: JUnit 5 / Testcontainers(PostgreSQL) / spring-security-test

## コマンド

- 起動: `./gradlew bootRun`(開発用のDB付き起動は `src/test` の `TestWebtodoApplication`)
- テスト: `./gradlew test`(Testcontainersを使うためDockerが必要)
- ビルド: `./gradlew build`

## パッケージ構成 (`com.example.webtodo`)

- `todo`: Entity / Repository / Service / Controller / Form
- `user`: 同上(ユーザー登録、`UserDetailsService`)
- `config`: `SecurityConfig` など設定クラス
- レイヤは Controller → Service → Repository の一方向。ControllerからRepositoryを直接呼ばない
- テンプレートは `templates/todo/`(画面)と `templates/fragments/`(htmx用部分)

## DB規約

- スキーマ変更は必ずFlyway: `src/main/resources/db/migration/V{n}__{description}.sql`
- 適用済みマイグレーションは編集しない。変更は新しいバージョンで追加する
- `spring.jpa.hibernate.ddl-auto=validate` のため、Entityとスキーマが不一致だと起動に失敗する

## コーディング規約

- LombokはEntityに `@Data` を使わず `@Getter` 等を個別に付ける
- 入力は `@Valid` + Bean Validation。Entityを画面に直接バインドせず、Formクラスを経由する
- UIメッセージは日本語

## セキュリティ規約

- パスワードはBCryptでハッシュ化して保存する
- TODOの参照・更新・削除は必ずログインユーザーの所有で絞り込む。他人のIDを指定された場合は404を返す
- CSRF保護は有効のまま。htmxリクエストにはCSRFトークンを `hx-headers` で付与する。`htmx-spring-boot-thymeleaf:5.1.0` の `hx:post` / `hx:put` / `hx:patch` / `hx:delete` は自動付与するため、これらを使う場合は手書きの `hx-headers` は不要
- 匿名アクセスを許可するのは `/login`、`/register`、`/error`、静的リソースのみ

## htmx規約

- 部分更新はフラグメントを返して該当要素(例: TODOの1行)のみ差し替える(`htmx-spring-boot-thymeleaf` を利用)
- 通常のリクエストとhtmxリクエストの両方で画面が壊れないようにする
- URL等の式を評価する属性は `hx:post="@{...}"` のように書く(`th:hx-post` ではない)。固定値は `hx-target` 等の通常の属性でよい
- htmx本体は WebJar(`org.webjars.npm:htmx.org:2.0.11`)で管理し、`fragments/head.html` の `<script defer>` で読み込む。サーバー側ライブラリだけではブラウザで動作しない

## フロントエンド規約

- Pico.cssの素のHTML(`<form>`、`<button>`、`<article>`、`<nav>` など)のスタイルを優先し、独自クラスは最小限にする
- 独自CSSは `static/css/app.css` にまとめ、Picoの変数(`--pico-*`)で上書きする
- レイアウト(`<head>`、ナビ)は `templates/fragments/` の共通フラグメントに切り出す
- 画面デザインの作り込みは機能実装の後に回す

## テスト方針

- Repository / Service: `@DataJpaTest` + Testcontainers
- Controller: `@WebMvcTest` + `spring-security-test`
- 「他ユーザーのTODOを操作できない」ケースを必ずテストする

## 実装の順序と進捗

- [x] 1. Flyway: `V1__create_users`、`V2__create_todos`
- [x] 2. user: `AppUser` / `UserRepository` / `AppUserDetailsService` / `UserService` / `RegisterForm` / `AuthController`
- [x] 3. `SecurityConfig`、`login.html`、`register.html`、`fragments/head.html`(登録・ログインは動作確認済み)
- [x] 4. todo: `Todo` / `TodoRepository` / `TodoForm` / `TodoNotFoundException` / `TodoService`、`AppUserPrincipal`(ログインユーザーIDとニックネームを保持)
- [x] 5. Controller・画面: 一覧 `GET /todos`、追加 `GET/POST /todos/new`・`form.html` は動作確認済み。ナビのニックネーム表示(`principal.name`)、共通レイアウト(`fragments/layout.html`)も実装済み。完了切替 `POST /todos/{id}/toggle` は実装済み・ブラウザのNetworkでPOSTと応答を動作確認済み。htmx時は1行のフラグメントを返し、通常リクエスト時は一覧へリダイレクトする。完了時の取り消し線(`<s>`)と期限切れの強調表示(`.overdue`)も実装・動作確認済み。編集 `GET/POST /todos/{id}/edit` も実装・動作確認済み。削除 `POST /todos/{id}/delete` も実装・動作確認済み(htmx時は200・本文なしで行を消し、通常時は303で一覧へ。204は使わない)
- [ ] 6. テスト(着手済み)
  - [x] `TodoTest`: `Todo#isOverdue` の4ケース
  - [x] `TodoServiceTest`: 作成・編集時の空白だけの説明を null で保存、本人のTODOの取得・削除・編集、本人の完了切替(未完了→完了、完了→未完了)、他ユーザーのTODOの取得・削除・編集・完了切替の拒否、一覧の所有者絞り込み・期限日の昇順・同じ期限日のID降順・期限なしが最後になること、他ユーザーのTODOだけが存在する場合に本人の一覧が空になること(計16ケース。ホスト側の `TodoServiceTest` 成功をユーザー確認済み)
  - [ ] Service / Repository: 必要に応じて残りの境界値・異常系テストを追加
  - [x] `TodoControllerTest`: 未ログイン時の一覧からログイン画面への転送、編集GETの404と正常表示、編集POSTの404・正常時の302リダイレクト・空白タイトルの入力エラー(フォーム再表示・todoId保持・Service未呼び出し)、削除POSTの404・通常時の303リダイレクト・htmx時の200/本文なし/Locationなし、完了切替POSTの404・通常時の302リダイレクト・htmx時の行フラグメントと完了済みタイトルの `<s>` 表示。各操作のServiceへのID受け渡しも検証(計12ケース。エージェント側で実行成功)
  - [ ] Controller: htmx完了切替の未完了表示、残りの入力境界値、追加・一覧の正常応答、CSRFトークンなしの拒否など

### 未解決・メモ

- `today`(`LocalDate.now()`)は `TodoController` の `@ModelAttribute("today")` で全ハンドラに入る。`todoItem.html` が `todo.isOverdue(today)` を使うため、フラグメントを返すハンドラが増えても渡し忘れは起きない
- 独自CSSは `static/css/app.css`。`fragments/head.html` でPico.cssの後ろに読み込む(読み込みを足さないと効かない)
- 期限切れの単体テスト(`Todo#isOverdue`)は実装・実行確認済み。固定日を基準に「昨日・未完了」「今日・未完了」「昨日・完了済み」「`dueDate` が null」の4ケースを検証
- `TodoServiceTest` は `@DataJpaTest` + `@Import({TodoService.class, TestcontainersConfiguration.class})` で既存のPostgreSQLコンテナ設定を利用する。別パッケージから参照するため `TestcontainersConfiguration` は public に変更済み
- 次回は `TodoControllerTest` の「htmxの完了切替で未完了の行フラグメントを返す」から再開する(未実装)。既存のhtmx完了切替テストを流用し、Serviceの `get` が返すTODOを `done=false` にして、タイトルが `<span>` に表示され、`<s>` がないことを検証する。Controllerでは実際の状態更新ではなく応答を検証し、DB上の双方向の完了切替はServiceテストが担当する
- 一覧の期限日昇順テストは「遅い・早い・中間」の順に3件作成し、「早い・中間・遅い」の順で返ることを検証する。2件を遅い・早いの順で作成するだけでは、ID降順のみの誤った実装でも通るため、3件で区別する
- 本人の編集・完了切替と、編集拒否・編集時の説明の空白→null変換は、`EntityManager.flush()` → `clear()` 後にDBから再取得して検証する。`flush()` はコミットではなく、`@DataJpaTest` の終了時にはロールバックされる
- 検証環境: エージェントの作業用DockerコンテナにはDockerソケットがなく、Testcontainersを起動できない。今回の全テスト `./gradlew test --rerun-tasks` は33件中17件(`TodoServiceTest` 16件と `WebtodoApplicationTests` 1件)がDocker環境エラーで検証処理前に失敗し、DBを使わない `TodoTest` 4件と `TodoControllerTest` 12件は成功。`TodoControllerTest` 12件の単独再実行も成功。`TodoServiceTest` の16ケース成功は以前ユーザーがホスト側で確認したもの。今回の全体の回帰成功は未確認のため、ホスト側で `JAVA_HOME=~/.sdkman/candidates/java/21.0.10-tem ./gradlew test` を実行する必要がある
- Controllerテストは `@WebMvcTest(TodoController.class)` + `@Import(SecurityConfig.class)`、Serviceは `@MockitoBean` で置き換える。未ログインの転送先は相対URL `/login`。`AppUser` のIDにはsetterがないため `ReflectionTestUtils.setField` で設定し、実際の `AppUserPrincipal` を `.with(user(principal))` で渡す。ControllerテストはServiceの例外のHTTP変換を検証するもので、実際のDB上の所有者判定はServiceテストが担当する
- 一覧のチェックボックスは有効化済み。`hx:post` + `hx-trigger="change"` + `hx-target="closest tr"` + `hx-swap="outerHTML"` で行を差し替える。テンプレート名は小文字の `fragments/todoItem.html`
- 一覧・追加画面は `layout(~{::main})` で共通レイアウトを利用する。渡す側の `<main>` に `class="container"` を付ける(`th:replace` でレイアウト側の `<main>` は置換される)
- トグルの404変換、通常リクエストの302リダイレクト、htmx時の完了済み行フラグメントはControllerテストで実装・実行確認済み。ブラウザ上での行の差し替えそのものはMockMvcの検証対象ではない
- ログインユーザーIDは `@AuthenticationPrincipal AppUserPrincipal` から `getId()` で取る。`@WebMvcTest` では `@WithMockUser` ではなく `AppUserPrincipal` を渡す(`SecurityMockMvcRequestPostProcessors.user(...)` など)
- ナビのログイン名は `sec:authentication="principal.name"`(ニックネーム)。`Principal` はログイン時点のスナップショットなので、ニックネーム変更機能を作る場合はセッション内の `Principal` も更新が必要
- `form.html` は追加・編集で共用。モデル属性 `todoId`(追加時は null)の有無で action・見出し・ボタン文言・タブ名を切り替える。`POST /todos/{id}/edit` のバリデーションエラー時も `todoId` をモデルに入れ直す(忘れると追加モードに戻る)
- 削除は `<form th:action hx:post hx-confirm>` の構成。htmxが無効でも通常のフォーム送信で動くが、その経路には確認ダイアログがない
- 「他ユーザーのTODOは404」のControllerテストは編集GET・編集POST・削除・完了切替で実装・実行確認済み。これらはモックServiceの例外からHTTP 404への変換とログインユーザーIDの受け渡しを検証する。実際の所有者制限はDBを使うServiceテストが担当し、Controllerテスト単独ではその証明にはならない
- `description` の空文字は Service で null に正規化する(`TodoService.normalizeDescriptionBlank`)
- 登録時の同時実行による `UNIQUE` 制約違反(`DataIntegrityViolationException`)の変換は未対応(任意)
