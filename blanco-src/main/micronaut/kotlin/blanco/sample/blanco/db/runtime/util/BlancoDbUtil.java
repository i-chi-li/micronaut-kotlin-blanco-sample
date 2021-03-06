/*
 * This code is generated by blanco Framework.
 */
package micronaut.kotlin.blanco.sample.blanco.db.runtime.util;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import micronaut.kotlin.blanco.sample.blanco.db.runtime.exception.DeadlockException;
import micronaut.kotlin.blanco.sample.blanco.db.runtime.exception.IntegrityConstraintException;
import micronaut.kotlin.blanco.sample.blanco.db.runtime.exception.TimeoutException;

/**
 * blancoDbが共通的に利用するユーティリティクラス。
 * このクラスはblancoDbが生成したソースコードで利用されます <br>
 * このクラスは blancoDbが生成したソースコードから利用されます。直接呼び出すことは推奨されません。
 * @since 2006.03.02
 * @author blanco Framework
 */
public class BlancoDbUtil {
    /**
     * SQL例外をblanco Framework例外オブジェクトに変換します。<br>
     * SQL例外のなかで、blanco Frameworkの例外オブジェクトに変換すべきものについて変換します。<br>
     * 変換すべき先が無い場合には、そのまま元のオブジェクトを返却します。
     *
     * @param ex JDBCから返却された例外オブジェクト。
     * @return 変換後のSQL例外オブジェクト。SQLExceptionまたはその継承クラスである IntegrityConstraintException, DeadlockException, TimeoutExceptionが戻ります。
     */
    public static SQLException convertToBlancoException(final SQLException ex) {
        if (ex.getSQLState() != null) {
            if (ex.getSQLState().startsWith("23")) {
                final IntegrityConstraintException exBlanco = new IntegrityConstraintException("データベース制約違反により変更が失敗しました。" + ex.toString(), ex.getSQLState(), ex.getErrorCode());
                exBlanco.initCause(ex);
                return exBlanco;
            } else if (ex.getSQLState().equals("40001")) {
                final DeadlockException exBlanco = new DeadlockException("データベースデッドロックにより変更が失敗しました。" + ex.toString(), ex.getSQLState(), ex.getErrorCode());
                exBlanco.initCause(ex);
                return exBlanco;
            } else if (ex.getSQLState().equals("HYT00")) {
                final TimeoutException exBlanco = new TimeoutException("データベースタイムアウトにより変更が失敗しました。" + ex.toString(), ex.getSQLState(), ex.getErrorCode());
                exBlanco.initCause(ex);
                return exBlanco;
            }
        }
        return ex;
    }

    /**
     * JDBCのTimestampをDate型に変換します。
     *
     * java.sql.Timestamp型からjava.util.Date型へと変換します。<br>
     * このメソッドは blancoDbが生成したソースコードから利用されます。直接呼び出すことは推奨されません。
     *
     * @param argTimestamp JDBCのTimestamp型を与えます。
     * @return 変換後のjava.util.Date型を戻します。
     */
    public static final Date convertTimestampToDate(final Timestamp argTimestamp) {
        if (argTimestamp == null) {
            return null;
        }
        return new Date(argTimestamp.getTime());
    }
}
