/*
 * このソースコードは blanco Frameworkにより自動生成されました。
 */
package micronaut.kotlin.blanco.sample.blanco.db.users.row;

import java.util.Date;

/**
 * SQL定義書(blancoDb)から作成された行クラス。
 *
 * 'C00S01UsersRow'行を表現します。
 * (1) 'user_id'列 型:int
 * (2) 'user_name'列 型:java.lang.String
 * (3) 'password'列 型:java.lang.String
 * (4) 'email'列 型:java.lang.String
 * (5) 'created_at'列 型:java.util.Date
 * (6) 'updated_at'列 型:java.util.Date
 */
public class C00S01UsersRow {
    /**
     * フィールド[user_id]です。
     *
     * フィールド: [user_id]。
     */
    private int fUserId;

    /**
     * フィールド[user_name]です。
     *
     * フィールド: [user_name]。
     */
    private String fUserName;

    /**
     * フィールド[password]です。
     *
     * フィールド: [password]。
     */
    private String fPassword;

    /**
     * フィールド[email]です。
     *
     * フィールド: [email]。
     */
    private String fEmail;

    /**
     * フィールド[created_at]です。
     *
     * フィールド: [created_at]。
     */
    private Date fCreatedAt;

    /**
     * フィールド[updated_at]です。
     *
     * フィールド: [updated_at]。
     */
    private Date fUpdatedAt;

    /**
     * フィールド [user_id] の値を設定します。
     *
     * フィールドの説明: [フィールド[user_id]です。]。
     *
     * @param argUserId フィールド[user_id]に設定する値。
     */
    public void setUserId(final int argUserId) {
        fUserId = argUserId;
    }

    /**
     * フィールド [user_id] の値を取得します。
     *
     * フィールドの説明: [フィールド[user_id]です。]。
     *
     * @return フィールド[user_id]から取得した値。
     */
    public int getUserId() {
        return fUserId;
    }

    /**
     * フィールド [user_name] の値を設定します。
     *
     * フィールドの説明: [フィールド[user_name]です。]。
     *
     * @param argUserName フィールド[user_name]に設定する値。
     */
    public void setUserName(final String argUserName) {
        fUserName = argUserName;
    }

    /**
     * フィールド [user_name] の値を取得します。
     *
     * フィールドの説明: [フィールド[user_name]です。]。
     *
     * @return フィールド[user_name]から取得した値。
     */
    public String getUserName() {
        return fUserName;
    }

    /**
     * フィールド [password] の値を設定します。
     *
     * フィールドの説明: [フィールド[password]です。]。
     *
     * @param argPassword フィールド[password]に設定する値。
     */
    public void setPassword(final String argPassword) {
        fPassword = argPassword;
    }

    /**
     * フィールド [password] の値を取得します。
     *
     * フィールドの説明: [フィールド[password]です。]。
     *
     * @return フィールド[password]から取得した値。
     */
    public String getPassword() {
        return fPassword;
    }

    /**
     * フィールド [email] の値を設定します。
     *
     * フィールドの説明: [フィールド[email]です。]。
     *
     * @param argEmail フィールド[email]に設定する値。
     */
    public void setEmail(final String argEmail) {
        fEmail = argEmail;
    }

    /**
     * フィールド [email] の値を取得します。
     *
     * フィールドの説明: [フィールド[email]です。]。
     *
     * @return フィールド[email]から取得した値。
     */
    public String getEmail() {
        return fEmail;
    }

    /**
     * フィールド [created_at] の値を設定します。
     *
     * フィールドの説明: [フィールド[created_at]です。]。
     *
     * @param argCreatedAt フィールド[created_at]に設定する値。
     */
    public void setCreatedAt(final Date argCreatedAt) {
        fCreatedAt = argCreatedAt;
    }

    /**
     * フィールド [created_at] の値を取得します。
     *
     * フィールドの説明: [フィールド[created_at]です。]。
     *
     * @return フィールド[created_at]から取得した値。
     */
    public Date getCreatedAt() {
        return fCreatedAt;
    }

    /**
     * フィールド [updated_at] の値を設定します。
     *
     * フィールドの説明: [フィールド[updated_at]です。]。
     *
     * @param argUpdatedAt フィールド[updated_at]に設定する値。
     */
    public void setUpdatedAt(final Date argUpdatedAt) {
        fUpdatedAt = argUpdatedAt;
    }

    /**
     * フィールド [updated_at] の値を取得します。
     *
     * フィールドの説明: [フィールド[updated_at]です。]。
     *
     * @return フィールド[updated_at]から取得した値。
     */
    public Date getUpdatedAt() {
        return fUpdatedAt;
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
        buf.append("micronaut.kotlin.blanco.sample.blanco.db.users.row.C00S01UsersRow[");
        buf.append("user_id=" + fUserId);
        buf.append(",user_name=" + fUserName);
        buf.append(",password=" + fPassword);
        buf.append(",email=" + fEmail);
        buf.append(",created_at=" + fCreatedAt);
        buf.append(",updated_at=" + fUpdatedAt);
        buf.append("]");
        return buf.toString();
    }
}
