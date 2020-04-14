/*
 * This code is generated by blanco Framework.
 */
package micronaut.kotlin.blanco.sample.blanco.db.runtime.exception;

/**
 * データベースの処理の結果、1行もデータが検索されなかったことを示す例外クラス <br>
 * このクラスはblancoDbが生成したソースコードで利用されます <br>
 * ※このクラスは、ソースコード自動生成後のファイルとして利用されます。
 * @since 2005.05.12
 * @author blanco Framework
 */
public class NoRowFoundException extends NotSingleRowException {
    /**
     * このクラスを表現するSQLStateコード。<br>
     * ※このクラスを利用する際には、SQLStateには頼らずに例外クラスの型によって状態を判断するようにしてください。
     */
    protected static final String SQLSTATE_NOROWFOUND = "00100";

    /**
     * データベースの処理の結果、1行もデータが検索されなかったことを示す例外クラスのインスタンスを生成します。
     *
     * @deprecated 理由を格納することができる別のコンストラクタを利用することを薦めます。
     */
    public NoRowFoundException() {
        super("No row found exception has occured.", SQLSTATE_NOROWFOUND);
    }

    /**
     * データベースの処理の結果、1行もデータが検索されなかったことを示す例外クラスのインスタンスを生成します。
     *
     * @param reason 例外の説明
     */
    public NoRowFoundException(final String reason) {
        super(reason, SQLSTATE_NOROWFOUND);
    }

    /**
     * データベースの処理の結果、1行もデータが検索されなかったことを示す例外クラスのインスタンスを生成します。
     *
     * @param reason 例外の説明
     * @param SQLState 例外を識別する XOPENコードまたは SQL 99のコード
     */
    public NoRowFoundException(final String reason, final String SQLState) {
        super(reason, SQLState);
    }

    /**
     * データベースの処理の結果、1行もデータが検索されなかったことを示す例外クラスのインスタンスを生成します。
     *
     * @param reason 例外の説明
     * @param SQLState 例外を識別する XOPENコードまたは SQL 99のコード
     * @param vendorCode データベースベンダーが定める固有の例外コード
     */
    public NoRowFoundException(final String reason, final String SQLState, final int vendorCode) {
        super(reason, SQLState, vendorCode);
    }
}
