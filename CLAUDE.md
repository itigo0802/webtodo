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
- CSRF保護は有効のまま。htmxリクエストにはCSRFトークンを `hx-headers` で付与する
- 匿名アクセスを許可するのは `/login`、`/register`、`/error`、静的リソースのみ

## htmx規約

- 部分更新はフラグメントを返して該当要素(例: TODOの1行)のみ差し替える(`htmx-spring-boot-thymeleaf` を利用)
- 通常のリクエストとhtmxリクエストの両方で画面が壊れないようにする

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
- [ ] 5. Controller・画面: 一覧 `GET /todos`(実装済み・動作確認済み)。追加 `GET/POST /todos/new`・`form.html`(動作確認済み)、ナビのニックネーム表示(`principal.name`)も実装済み。残りは編集、`POST /todos/{id}/toggle`、削除、期限切れの強調表示
- [ ] 6. テスト

### 未解決・メモ

- 一覧のチェックボックスはトグル実装まで `disabled` にする。`<main>` に `class="container"` を付ける
- ログインユーザーIDは `@AuthenticationPrincipal AppUserPrincipal` から `getId()` で取る。`@WebMvcTest` では `@WithMockUser` ではなく `AppUserPrincipal` を渡す(`SecurityMockMvcRequestPostProcessors.user(...)` など)
- ナビのログイン名は `sec:authentication="principal.name"`(ニックネーム)。`Principal` はログイン時点のスナップショットなので、ニックネーム変更機能を作る場合はセッション内の `Principal` も更新が必要
- `form.html` は現在追加専用(`th:action="@{/todos/new}"`、ボタン文言は「追加」直書き)。編集実装時にモデル経由で action・見出し・ボタン文言を切り替える
- `description` の空文字は Service で null に正規化する(`TodoService.normalizeDescriptionBlank`)
- 登録時の同時実行による `UNIQUE` 制約違反(`DataIntegrityViolationException`)の変換は未対応(任意)
