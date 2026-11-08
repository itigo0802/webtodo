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

## 参考ドキュメント

- テストの組み方・落とし穴・画面まわりの知見・環境メモは `docs/pitfalls.md` に分離してある。常時読み込ませないためこのファイルには置かない。テストを書く・実行する前に読む。

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
  - [x] `TodoControllerTest`: 未ログイン時の一覧からログイン画面への転送、編集GETの404と正常表示、編集POSTの404・正常時の302リダイレクト・空白タイトルの入力エラー(フォーム再表示・todoId保持・Service未呼び出し)、削除POSTの404・通常時の303リダイレクト・htmx時の200/本文なし/Locationなし、完了切替POSTの404・通常時の302リダイレクト・htmx時の行フラグメントと完了済みタイトルの `<s>` 表示・未完了タイトルの `<span>` 表示と `<s>` 不在、追加・編集・削除・完了切替POSTのCSRFトークンなしでの403とService未呼び出し、追加POSTの正常時の302リダイレクトと全角スペースだけのタイトルの入力エラー、追加GETの正常表示、空一覧のメッセージとTODO入り一覧のタイトル表示、追加POSTのタイトル上限の境界(101文字の拒否と `todoForm.title` のフィールドエラー・ちょうど100文字の受け付けと302リダイレクト)、説明上限の境界(1001文字の拒否と `Size` エラーコード・ちょうど1000文字の受け付けと302リダイレクト)、追加POSTの期限日の形式不正(`typeMismatch` のフィールドエラーでフォーム再表示)と正常形式が `LocalDate` としてServiceへ渡ること、編集画面のtitle・dueDate・descriptionの復元、一覧のチェックボックスへのhtmxトグルURLとCSRFヘッダ(`hx-headers` / `X-CSRF-TOKEN`)の描画、一覧で期限が昨日の未完了TODOが `overdue` として描画されること(対になる「完了済みなら描画されない」も)、一覧の削除フォームの通常送信 `action` と `hx-confirm` の描画。対象操作のServiceへのユーザーID・TODO ID受け渡しも検証(計32ケース。エージェント側で実行成功)
  - [ ] Controller: 編集POST側の入力境界値(追加側と同じ `TodoForm` の制約を通るため優先度は低い)、未ログイン時の一覧以外のエンドポイントの転送(GETは302で `/login`、POSTはCSRFフィルタが先に働いて403になるので「未ログインだから403」とは言い切れない)、一覧行の削除フォームの `hx-post`(配線そのものは未検証。`action` と `hx-confirm` は検証済み)、`hx-target` / `hx-swap` が差し替え対象の行を指し続けていること
  - [ ] `user`: `AuthController` の登録GET/POST、重複メール(`DuplicateEmailException`)の `rejectValue` 変換、`UserService` のBCrypt保存、`RegisterForm` の制約、`AppUserDetailsService`(いずれも未着手。`src/test` に `user` 配下のテストが1本も無い)
