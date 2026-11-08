# テストと実装の知見 (pitfalls)

`CLAUDE.md` から分離した作業メモ。常時読み込む必要はないが、テストを書く・実行する前に読むと手戻りが減る知見を置く。

## テストの組み方

- `TodoServiceTest`: `@DataJpaTest` + `@Import({TodoService.class, TestcontainersConfiguration.class})` で既存のPostgreSQLコンテナ設定を利用する。別パッケージから参照するため `TestcontainersConfiguration` は public。
- `TodoControllerTest`: `@WebMvcTest(TodoController.class)` + `@Import(SecurityConfig.class)`、Service は `@MockitoBean` で置き換える。
- Controller テストでログインユーザーを渡すときは `@WithMockUser` ではなく実物の `AppUserPrincipal` を作り `SecurityMockMvcRequestPostProcessors.user(principal)` で渡す。`AppUser` の id に setter が無いので `ReflectionTestUtils.setField` で設定する。
- 未ログイン時の転送先は相対URL `/login`。
- Controller テストのフィクスチャの email は `user1@example.com` に揃える(`AppUser`/`AppUserPrincipal` 側に検証が入ると、値の揺れが原因の分かりにくい失敗になる)。

## テストの落とし穴

- MockMvc の `xpath` はXMLパーサーを使うため、`<!doctype html>` で始まる一覧ページ全体では解析エラーになる。ページ全体の本文検証は `content().string(containsString(...))`(Hamcrest をstatic import)。ただしこれは文字列の存在確認でDOM位置の証明ではない。行フラグメント(整形式XML)のXPath検証は使える。
- 型変換エラーの判定: `attributeHasFieldErrorCode` は `FieldError#getCode()` と完全一致で比較する(spring-test 7.0.9)。型変換エラーの先頭コードは `typeMismatch.todoForm.dueDate` のように長いので `containsString("typeMismatch")` のMatcher版を使う。`@NotBlank`/`@Size` 由来の `Size` などとは別系統。
- `verify(todoService).create(any(TodoForm.class), eq(1L))` は「呼ばれた」しか見ないので引数の中身は未検証になる。値まで見るときは `ArgumentCaptor<TodoForm>` で捕まえる。`captor.capture()` は matcher なので、同じ呼び出しの他の引数も `eq(1L)` のように matcher に揃える(生値を混ぜると `InvalidUseOfMatchersException`)。
- 並び順のテストは3件必要。「遅い・早い」の2件だけだと ID 降順のみの誤実装でも通る。遅い・早い・中間の3件を作り、早い・中間・遅いの順で返ることを見る。
- DB 書き込みを伴う検証は `EntityManager.flush()` → `clear()` してから再取得する。`flush()` はコミットではなく、`@DataJpaTest` の終了時にロールバックされる。

## テストの原則

- 描画のテストは肯定と否定の2本セットで意味が出る。片方だけだと「常にその文字列を書く」「該当属性を消す」どちらの実装でも通ってしまう。
- テンプレートから対象属性を一時的に外す変異テストで、意図した assert だけが赤になることを確認できる(変異は `git checkout --` で戻す)。
- Controller テストが検証できるのは「モック Service の例外・戻り値の HTTP 変換」と「ログインユーザーIDの受け渡し」まで。実際の所有者制限はDBを使う Service テストが担当する。Controller テスト単独では所有者制限の証明にならない。
- ブラウザ上での htmx の行差し替え自体は MockMvc の検証対象ではない。

## テストの実行

- 全件: `./gradlew test --rerun-tasks`
- 個別: `./gradlew test --tests com.example.webtodo.todo.TodoControllerTest --rerun-tasks`
- 件数は `build/test-results/test/*.xml` を読んで確認する(BUILD SUCCESSFUL だけでは件数が分からない)。

## 画面・テンプレート

- `form.html` は追加・編集で共用。モデル属性 `todoId`(追加時 null)の有無で action・見出し・ボタン文言・タブ名を切り替える。`POST /todos/{id}/edit` のバリデーションエラー時も `todoId` をモデルに入れ直す(忘れると追加モードに戻る)。
- 一覧・追加画面は `layout(~{::main})` で共通レイアウトを使う。渡す側の `<main>` に `class="container"` を付ける(`th:replace` でレイアウト側の `<main>` は置換される)。
- 一覧のチェックボックスは `hx:post` + `hx-trigger="change"` + `hx-target="closest tr"` + `hx-swap="outerHTML"` で行を差し替える。テンプレート名は小文字の `fragments/todoItem.html`。
- 削除は `<form th:action hx:post hx-confirm>` の構成。htmx 無効でも通常送信で動くが、その経路には確認ダイアログが無い。`th:action` が消えると JS 無効・htmx 不達時に削除できなくなり、`hx-confirm` が消えると確認なしの即削除になる。両方の描画を Controller テストで固定している。
- 独自CSSは `static/css/app.css` を `fragments/head.html` で Pico.css の後ろに読み込む(読み込みを足さないと効かない)。
- `today`(`LocalDate.now()`)は `TodoController` の `@ModelAttribute("today")` で全ハンドラに入る。`todoItem.html` が `todo.isOverdue(today)` を使うため、フラグメントを返すハンドラが増えても渡し忘れは起きない。
- 一覧ページのHTMLに `overdue` の文字列が出るのは `todoItem.html` の `th:classappend` 1箇所だけ(`app.css` は外部リンクなので本文には出ない)。よって `containsString("overdue")` で該当行の描画を特定できる。

## 設計メモ

- `description` の空文字は Service で null に正規化する(`TodoService.normalizeDescriptionBlank`)。
- ナビのログイン名は `sec:authentication="principal.name"`(ニックネーム)。`Principal` はログイン時点のスナップショットなので、ニックネーム変更機能を作る場合はセッション内の `Principal` の更新も必要。
- htmx の CSRF 自動付与の実体: `htmx-spring-boot-thymeleaf:5.1.0` の `HtmxCsrfAttributeProcessor` が `_csrf` リクエスト属性から `CsrfToken#getHeaderName()`/`getToken()` を取って `hx-headers` を描画する。`th:action` の隠し `_csrf` 入力とは別経路なので、一覧ページのHTMLに `X-CSRF-TOKEN` が出ていればそれは `hx:post` の描画によるもの。

## 未解決・未対応

- 登録時の同時実行による `UNIQUE` 制約違反(`DataIntegrityViolationException`)の変換は未対応(任意)。
- `user` パッケージのテストが1本も無い。次回はここから再開し、範囲が広いので1件ずつ進める。
  - 1件目: `AuthController` の登録POSTを `@WebMvcTest(AuthController.class)` + `@Import(SecurityConfig.class)` + `@MockitoBean UserService` で組む(`TodoControllerTest` と同じ組み方)。匿名許可済みの `/register` へCSRF付きで送り、正常時に `redirect:/login`(302) へ転送され、email・password・name が `RegisterForm` として Service へ渡ることを見る。
  - `RegisterForm` の制約: email `@NotBlank`/`@Email`/`@Size(max=255)`、password `@NotBlank`/`@Size(min=8, max=72)`、name `@NotBlank`/`@Size(max=50)`。エラー時は `register` ビューへ戻る(モデル属性名 `registerForm`)。
  - 重複メールは `DuplicateEmailException` を `result.rejectValue("email", "duplicate", ...)` に変換して同じく再表示する。
- Controller 側の残テスト: 編集POSTの入力境界値(追加側と同じ `TodoForm` の制約を通るため優先度は低い)、未ログイン時の一覧以外のエンドポイントの転送(GETは302で `/login`、POSTはCSRFフィルタが先に働いて403になるので「未ログインだから403」とは言い切れない)、一覧行の削除フォームの `hx-post` の配線、`hx-target`/`hx-swap` が差し替え対象の行を指し続けていること。

## 環境

- エージェントの作業先は Cua VM 内の `/home/cua/webtodo`。ホストの `/home/itigo0802/git/webtodo` とは別チェックアウトで自動同期しない。別チェックアウトから push された場合は VM 側で `git pull --ff-only` で取り込む。
- VM のチェックアウトは `gh auth git-credential` を helper に使い、`git push` も `git fetch origin` も通る。`git fetch --all` は `entire` リモートがOSキーリング(`org.freedesktop.secrets`)を要求して失敗するので、GitHub だけ取り込むときは `git fetch origin` を使う。
