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

## blancoDB でのソースコード自動生成方法
blancoDB でのソースコード自動生成方法は、以下のようにする。

- MySQL を起動
- mysql -uroot < SQL/DB_Initial.sql
- gradlew blancoDb
