/*
 * このソースコードは blanco Frameworkにより自動生成されました。
 */
package micronaut.kotlin.blanco.sample.blanco.db.users.row;

/**
 * SQL定義書(blancoDb)から作成された行クラス。
 *
 * 'C00S02UsersRow'行を表現します。
 * (1) '1'列 型:java.lang.String
 */
public class C00S02UsersRow {
    /**
     * フィールド[1]です。
     *
     * フィールド: [1]。
     */
    private String f1;

    /**
     * フィールド [1] の値を設定します。
     *
     * フィールドの説明: [フィールド[1]です。]。
     *
     * @param arg1 フィールド[1]に設定する値。
     */
    public void set1(final String arg1) {
        f1 = arg1;
    }

    /**
     * フィールド [1] の値を取得します。
     *
     * フィールドの説明: [フィールド[1]です。]。
     *
     * @return フィールド[1]から取得した値。
     */
    public String get1() {
        return f1;
    }

    /**
     * このバリューオブジェクトの文字列表現を取得します。
     *
     * <P>使用上の注意</P>
     * <UL>
     * <LI>オブジェクトのシャロー範囲のみ文字列化の処理対象となります。
     * <LI>オブジェクトが循環参照している場合には、このメソッドは使わないでください。
     * </UL>
     *
     * @return バリューオブジェクトの文字列表現。
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("micronaut.kotlin.blanco.sample.blanco.db.users.row.C00S02UsersRow[");
        buf.append("1=" + f1);
        buf.append("]");
        return buf.toString();
    }
}
