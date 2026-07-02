# 使ったプロンプトと評価

演習中に Codex CLI に投げた主要なプロンプトを記録する。
`EXERCISES.html` に記載の機能実装や、修正提案に関するプロンプトを記述する。

## 書き方

- どのフェーズで使ったか (例: 投稿一覧、いいね機能、リファクタ)
- 実際に投げたプロンプト本文 (省略せずコピペ)
- 結果: 効いた / 部分的に効いた / 効かなかった
- 振り返り: 次に同じ状況が来たらどう変えるか

---

## プロンプト 1(サンプル)

**フェーズ**: サンプルデータ(シーダー)の追加

**プロンプト本文**:

```
src\main\resources\db\migration\V1__init.sql に基づくH2用のシーダーを作成し、実行する。テストデータとして10個程度のシードデータを作成。H2で起動時に自動投入する。
```

**結果**: 効いた

```
• 提案どおり、H2 プロファイル起動時だけ seed が自動適用される構成にしました。

  差分:

  - src/main/resources/application-h2.yml:20: H2 の Flyway location に classpath:db/seed/h2 を追加。
  - src/main/resources/db/seed/h2/R__seed_posts_h2.sql:6: posts に 10 件投入する H2 専用 repeatable migration を追加。posts_seq は次
    ID が 11 になるよう更新。

  - src/test/resources/application-h2.yml:17: テストは空 DB 前提を維持するため seed location を外す設定を追加。
  - src/test/java/com/example/tsubuyaki/db/H2SeedMigrationTest.java:18: seed 適用で 10 件入ることと、次 sequence 値が 11 になることを
    検証。

  検証:

  - .\mvnw -B -Dmaven.repo.local=C:/home/codex/.m2/repository -Dtest=H2SeedMigrationTest test 成功
  - .\mvnw -B -Dmaven.repo.local=C:/home/codex/.m2/repository -Ph2 spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-
    application-type=none" 成功。ログで Migrating schema "PUBLIC" with repeatable migration "seed posts h2" を確認。

  - .\mvnw -B -Dmaven.repo.local=C:/home/codex/.m2/repository -Ph2 verify 成功。警告: Rule violated for bundle tsubuyaki-board: lines
    covered ratio is 0.48, but expected minimum is 0.60。日本語要約: 現在のカバレッジは 48% で 60% 未満ですが、このプロファイルでは
    fail 設定ではないためビルドは成功しています。
```

**振り返り**:

シーダーは追加できた。
検証方法を指定しなかったためハーネスであらかじめ定義された検証方法を自動的に起動した。
CI検証は成功しているが、実際に画面表示の確認はできていない(未実装のため)。

---

## プロンプト 2

**フェーズ**:M3対応

**プロンプト本文**:

```
投稿登録機能を作成する。
    - 入力項目は PostForm の author, body を使う
    - 投稿者 author は必須、30文字まで、空白のみはNG
    - 本文 body は必須、280文字まで、空白のみはNG
    - 登録後、投稿一覧画面で登録した投稿が見えること

    以下の観点のテストを用意してから本機能の実装を行う。
    - GET /posts/new で postForm が model に積まれ、posts/form.html を表示できること
    - author が空文字または空白のみの場合、登録せず posts/form を再表示しエラーを表示すること
    - author が31文字以上の場合、登録せず posts/form を再表示しエラーを表示すること
    - body が空文字または空白のみの場合、登録せず posts/form を再表示しエラーを表示すること
    - body が281文字以上の場合、登録せず posts/form を再表示しエラーを表示すること
    - 入力が正常な場合、PostService が投稿者・本文・投稿日を保存すること
    - 入力が正常な場合、POST /posts 後に /posts へリダイレクトすること
    - 登録後、GET /posts で登録した投稿が一覧に表示されること
```

**結果**: 効いた

```
M3 投稿登録機能を実装しました。

  差分:

  - 成功時は PostService.create(...) で保存し、redirect:/posts
  - src/main/java/com/example/tsubuyaki/service/PostService.java:33 で createdAt = Instant.now() を設定して
    保存

      - PostServiceTest
      - PostCreateFlowTest

  検証:

  - ./mvnw -B -Ph2 -Dtest=PostControllerTest,PostServiceTest,PostCreateFlowTest test 成功、16件
  - ./mvnw -B -Ph2 verify 成功、22件
  - Checkstyle 0、SpotBugs 0、JaCoCo OK
```

**振り返り**:

ロジック的な間違いはなく、実際にローカル起動し動作を確認できた。
PostCreateFlowTestは今回の研修では余分なケースと思ったので、
指摘し、別のプロンプトで削除することもできた。


---

## プロンプト 3

**フェーズ**:M4対応

**プロンプト本文**:

```
投稿詳細表示機能を作成する。
    - GET /posts/{id} で投稿詳細を表示する
    - ビューは posts/detail.html を新規作成する
    - 一覧に戻るリンクを設ける
    - 内容は、適度な文字数で折り返す
    - XSS 対策として Thymeleaf では th:text を使う

    以下の観点のテストを用意してから本機能の実装を行う。
    - GET /posts/{id} で存在する投稿の詳細画面 posts/detail.html を表示できること
    - model.post 属性に投稿を積めていること
    - 投稿者, 内容, 投稿日 の順に表示できていること
    - 存在しない id の場合に 404 を返すこと
    - 一覧に戻るリンクがあり、/posts に遷移できること
    - 内容が HTML として解釈されず、エスケープ表示されること
```

**結果**: 部分的に効いた

```
M4 投稿詳細表示機能を実装しました。

  差分:

  - GET /posts/{id} を PostController に追加
  - PostService.findById(Long id) を追加
  - 存在しない id は 404
  - 一覧に戻るリンクを追加
  - 本文は th:text でエスケープ表示

  - ./mvnw -B -Ph2 -Dtest=PostControllerTest,PostServiceTest test 成功、22件
  - ./mvnw -B -Ph2 verify 成功、28件
  - Checkstyle 0、SpotBugs 0、JaCoCo OK
```

**振り返り**:

一覧画面に「詳細」ボタンを作成するというプロンプトが漏れており、
作成された詳細画面へ遷移する方法がない実装になっていた。
機能としては明らかにおかしいが、AI自身はそこに気づけないというのは弱点だと思った。

---

## プロンプト 4

**フェーズ**:S1対応

**プロンプト本文**:

```
いいね機能を作成する。
        - 対象は投稿詳細画面の投稿
        - POST /posts/{id}/likes でいいねをトグルする
        - 同一 clientHash が初回に押した場合はいいねを追加する
        - 同一 clientHash が再度押した場合はいいねを解除する
        - clientHash は IP + User-Agent を SHA-256 でハッシュ化し、先頭8文字を使う
        - 投稿詳細画面でいいね数と Like ボタンが見えること
        - いいね操作後は /posts/{id} にリダイレクトすること

        以下の観点のテストを用意してから本機能の実装を行う。
        - POST /posts/{id}/likes で対象投稿にいいねを追加できること
        - 同一 clientHash で再度 POST /posts/{id}/likes した場合、いいねを解除できること
        - clientHash が IP + User-Agent から SHA-256 先頭8文字で生成されること
        - 異なる clientHash からのいいねは別ユーザーのいいねとして加算されること
        - 存在しない投稿 id に POST /posts/{id}/likes した場合、404 を返すこと
        - 投稿詳細 GET /posts/{id} でいいね数が model に積まれること
        - 投稿詳細画面でいいね数と Like ボタンが表示されること
        - いいね操作後、/posts/{id} へリダイレクトすること
        - いいね追加後、GET /posts/{id} でいいね数が増えて表示されること
        - いいね解除後、GET /posts/{id} でいいね数が減って表示されること
```

**結果**: 部分的に効いた

```
対応しました。テスト実行中だった ./mvnw -B -Ph2 -Pcoverage-day2 ... verify は、指示どおり停止済みです。

  実装内容:

  - POST /posts/{id}/likes を追加
  - 同一 clientHash のいいね追加/解除トグルを実装
  - IP + User-Agent を SHA-256 化し、先頭8文字を clientHash として生成
  - 詳細画面に いいね数 と Like ボタンを表示
  - post_likes テーブル用 Flyway migration V2__post_likes.sql を追加
  - Repository / Service / Controller / 結合テストを追加・更新
  - PostNotFoundException.java
  - V2__post_likes.sql
  - PostLikeRepositoryTest.java
  - ClientHashGeneratorTest.java
  確認済み:

  - S1対象テストは一度 Tests run: 34, Failures: 0, Errors: 0 まで通過

  未完了:

  - 最後の -Pcoverage-day2 の JaCoCo 70% gate は、実行途中で停止したため最終確認未完了です。
  - git status は /home/codex/.gitconfig の Permission denied で確認できませんでした。
```

**振り返り**:

本来ボタンではなく、トグルである要件だったが、指示誤りでボタンで実装された。
この後、別途トグルで作成してほしい旨を指示するとその通りに実装してくれた。

---

## プロンプト 5

**フェーズ**:S2対応

**プロンプト本文**:

```
キーワード検索機能を作成する。
    - GET /posts?q=xxx で投稿本文をキーワード検索する
    - 検索対象は投稿本文とし、部分一致で検索する
    - 検索後も入力したキーワードを検索ボックスに表示する
    - SQL / JPQL の文字列連結は禁止し、Spring Data JPA のメソッド名クエリまたは bind 変数を使う
    - sample 配下のテストは修正しない

    以下の観点のテストを用意してから本機能の実装を行う。
    - 検索キーワードを model.q に積めていること
    - 検索後の画面に検索ボックスが表示され、入力値が保持されること
    - q が未指定の場合は従来どおり最新50件を表示すること
    - q が空文字または空白のみの場合も従来どおり最新50件を表示すること
    - 検索結果が 0 件の場合、空メッセージを表示できること
    - Repository で本文の部分一致検索ができること
    - Service で q の有無に応じて最新一覧と検索結果を切り替えられること
    - 最後に ./mvnw -B -Ph2 verify が成功すること
```

**結果**: 効いた

```
S2 キーワード検索を実装しました。

  変更内容:

  - src/main/java/com/example/tsubuyaki/controller/PostController.java:29: GET /posts?q=... を受け取り、model.posts と
    model.q を設定

  - src/main/java/com/example/tsubuyaki/service/PostService.java:39: q 未指定・空白なら最新50件、指定ありなら本文検索へ
    分岐

  - src/main/java/com/example/tsubuyaki/repository/PostRepository.java:12: Spring Data JPA メソッド名クエリで本文部分一
    致検索を追加

  - src/main/resources/templates/posts/list.html: 検索ボックスを追加し、検索後の入力値を保持
  - Controller / Service / Repository テストを追加・更新

  sample 配下は修正していません。

  検証:

  - ./mvnw -B -Ph2 -Dtest=PostRepositoryTest,PostServiceTest,PostControllerTest test 成功
  - ./mvnw -B -Ph2 verify 成功
  - Checkstyle 違反 0、SpotBugs 警告 0、JaCoCo coverage check 成功

  補足: 途中で既存の target/jacoco.exec が壊れていたため、生成物の JaCoCo 実行データだけ削除して作り直しました。
```

**振り返り**:

問題なく想定通りに実装してくれた

---

## プロンプト 6

**フェーズ**:S3対応

**プロンプト本文**:

```
投稿者名フィールド拡張機能を作成する。

    - 投稿フォームに任意のアバター色を選択できる項目を追加する
    - アバター色は未選択でも投稿できる
    - アバター色を選択した場合、投稿一覧と投稿詳細で投稿者名の横に色付きの丸を表示する
    - sample 配下のテストは修正しない

    以下の観点のテストを用意してから本機能の実装を行う。

    - 投稿作成フォーム表示時に、投稿者名入力欄とアバター色選択欄が表示されること
    - 投稿者名が空文字の場合、フォームを再表示しエラーを表示すること
    - 投稿者名が空白のみの場合、フォームを再表示しエラーを表示すること
    - 投稿者名が 31 文字以上の場合、フォームを再表示しエラーを表示すること
    - アバター色が未選択でも投稿を保存できること
    - アバター色を選択した場合、投稿と一緒に保存できること
    - 許可されていないアバター色が送信された場合、フォームを再表示しエラーを表示すること
    - 投稿一覧で、アバター色がある投稿は投稿者名の横に色付きの丸を表示できること
    - 投稿詳細で、アバター色がある投稿は投稿者名の横に色付きの丸を表示できること
    - 投稿一覧と投稿詳細で、投稿者名が HTML として解釈されずエスケープ表示されること
    - Repository / Service / Controller のテストを追加または更新すること
    - 最後に ./mvnw -B -Ph2 verify が成功すること
```

**結果**: 効いた

```
S3 投稿者名フィールド拡張を実装しました。

  主な変更:

  - Post に avatarColor を追加し、V3__post_avatar_color.sql で avatar_color 列と許可値制約を追加
  - PostForm に avatarColor を追加し、blue/green/pink/yellow だけ許可
  - 投稿フォームにアバター色のセレクトボックスを追加
  - 投稿一覧・詳細で、アバター色がある場合に投稿者名の横へ色付き丸を表示
  - 任意 CSS 値は使わず、保存トークンから固定 CSS クラスだけを出力
  - Repository / Service / Controller テストを追加・更新
  - sample 配下は未修正

  検証:

  ./mvnw -B -Ph2 -Dtest=PostRepositoryTest,PostServiceTest,PostControllerTest test
  BUILD SUCCESS

  ./mvnw -B -Ph2 verify
  BUILD SUCCESS

  Checkstyle 違反 0、SpotBugs 警告 0、JaCoCo coverage check 成功です。
```

**振り返り**:

問題なく想定通りに実装してくれた（選択可能な色はこちらの指示がなくても、codexが自動で決めてくれた）

---

## プロンプト 7

**フェーズ**:C2対応

**プロンプト本文**:

```
投稿削除機能を作成する。

    - 投稿は物理削除せず、deleted_at カラムを使って論理削除する
    - 投稿一覧では、論理削除済みの投稿を表示しない
    - 投稿詳細では、論理削除済みの投稿は 404 を返す
    - 投稿詳細画面に削除ボタンを追加する
    - DELETE /posts/{id} または POST /posts/{id}/delete で投稿を削除する
    - 削除後は /posts にリダイレクトする
    - 存在しない id の削除リクエストは 404 を返す
    - sample 配下のテストは修正しない

    以下の観点のテストを用意してから本機能の実装を行う。

    - Repository で deleted_at が null の投稿だけを新着順で取得できること
    - Repository で deleted_at が設定された投稿は検索対象から除外されること
    - Service で投稿を物理削除せず deleted_at を設定できること
    - Service で存在しない id を削除しようとした場合、PostNotFoundException を投げること
    - GET /posts で論理削除済みの投稿が model.posts に含まれないこと
    - GET /posts/{id} で論理削除済みの投稿は 404 を返すこと
    - 投稿詳細画面に削除ボタンが表示されること
    - 削除ボタン押下後、/posts にリダイレクトすること
    - 存在しない id に削除リクエストを送った場合、404 を返すこと
    - Flyway migration で posts.deleted_at カラムを追加すること
    - 最後に ./mvnw -B -Ph2 verify が成功すること
```

**結果**: 効いた

```
  主な変更:

  - posts.deleted_at 追加 migration: V4__post_deleted_at.sql
  - Post に deletedAt と markDeleted(...) を追加
  - 一覧・検索・詳細取得を deletedAt IS NULL の投稿だけに変更
  - POST /posts/{id}/delete を追加
  - 詳細画面に削除ボタンを追加
  - Repository / Service / Controller テストを追加・更新
  - Spring DI に対する SpotBugs 誤検知を config/spotbugs-exclude.xml で除外

  検証:

  - ./mvnw -B -Ph2 -Dtest=PostRepositoryTest,PostServiceTest,PostControllerTest test 成功
  - ./mvnw -B -Ph2 verify 成功
  - 全体: 64 tests, failures 0, Checkstyle 0, SpotBugs 0, JaCoCo OK
```

**振り返り**:

問題なく想定通りに実装してくれた

---