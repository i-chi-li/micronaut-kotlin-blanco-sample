# micronaut-kotlin-blanco-sample
このプロジェクトは、Kotlin 、Micronaut および、blancoDB を組み合わせて利用する場合のサンプルとなる。

## 前提
サンプルコードを動作させるには、MySQL がローカル環境で起動している必要がある。
MySQL 設定ファイルのサンプルとして SQL/my.ini を利用できる。
データディレクトリは、任意のディレクトリに変更する。

```
# データベースの初期化（初回のみ実行）
mysqld --initialize-insecure
# MySQL をコンソールモード（フォアグラウンド実行）で起動
mysqld --console
```

接続ユーザは、root でパスワード無し。
DB 初期化には、SQL/DB_Initial.sql を利用する。

作成する Database は、sample00 となり、
テーブルは、users となる。

## ディレクトリ構成
blancoDB で参照する、SQL 定義書は、meta/db ディレクトリに格納する。
自動生成ソースコードは、blanco/main/ ディレクトリ以下に生成される。

- blanco-libs/  
  blancoDB のソース自動生成時に利用するライブラリを格納
- blanco-meta/db/  
  blancoDB の SQL 定義書を格納
- blanco-src/  
  blancoDB で自動生成したソースを格納
- SQL/  
  MySQL のテーブル定義 SQL を格納

## blancoDB でのソースコード自動生成方法
blancoDB でのソースコード自動生成方法は、以下のようにする。

- MySQL を起動
- mysql -uroot < SQL/DB_Initial.sql
- gradlew blancoDb

## サンプルコード

- [DB 検索・登録・更新](src/main/kotlin/micronaut/kotlin/blanco/sample/UsersController.kt)
- [エラーハンドリング](src/main/kotlin/micronaut/kotlin/blanco/sample/GlobalHandlerController.kt)
- [日時変換処理](src/main/kotlin/micronaut/kotlin/blanco/sample/DateController.kt)
