/*
 * このソースコードは blanco Frameworkによって自動生成されています。
 * リソースバンドル定義書から作成されたリソースバンドルクラス。
 */
package micronaut.kotlin.blanco.sample.blanco.resourcebundle;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * リソースバンドル定義[Common]のリソースバンドルクラス。
 *
 * このクラスはリソースバンドル定義書から自動生成されたリソースバンドルクラスです。<BR>
 * 既知のロケール<BR>
 * <UL>
 * <LI>ja
 * </UL>
 */
public class CommonResourceBundle {
    /**
     * リソースバンドルオブジェクト。
     *
     * 内部的に実際に入力を行うリソースバンドルを記憶します。
     */
    private ResourceBundle fResourceBundle;

    /**
     * CommonResourceBundleクラスのコンストラクタ。
     *
     * 基底名[Common]、デフォルトのロケール、呼び出し側のクラスローダを使用して、リソースバンドルを取得します。
     */
    public CommonResourceBundle() {
        try {
            fResourceBundle = ResourceBundle.getBundle("micronaut/kotlin/blanco/sample/blanco/resourcebundle/Common");
        } catch (MissingResourceException ex) {
        }
    }

    /**
     * CommonResourceBundleクラスのコンストラクタ。
     *
     * 基底名[Common]、指定されたロケール、呼び出し側のクラスローダを使用して、リソースバンドルを取得します。
     *
     * @param locale ロケールの指定
     */
    public CommonResourceBundle(final Locale locale) {
        try {
            fResourceBundle = ResourceBundle.getBundle("micronaut/kotlin/blanco/sample/blanco/resourcebundle/Common", locale);
        } catch (MissingResourceException ex) {
        }
    }

    /**
     * CommonResourceBundleクラスのコンストラクタ。
     *
     * 基底名[Common]、指定されたロケール、指定されたクラスローダを使用して、リソースバンドルを取得します。
     *
     * @param locale ロケールの指定
     * @param loader クラスローダの指定
     */
    public CommonResourceBundle(final Locale locale, final ClassLoader loader) {
        try {
            fResourceBundle = ResourceBundle.getBundle("micronaut/kotlin/blanco/sample/blanco/resourcebundle/Common", locale, loader);
        } catch (MissingResourceException ex) {
        }
    }

    /**
     * 内部的に保持しているリソースバンドルオブジェクトを取得します。
     *
     * @return 内部的に保持しているリソースバンドルオブジェクト。
     */
    public ResourceBundle getResourceBundle() {
        return fResourceBundle;
    }

    /**
     * bundle[Common], key[I001]
     *
     * [こんにちは {0}] (ja)<br>
     *
     * @param arg0 置換文字列{0}を置換する値。java.lang.String型を与えてください。
     * @return key[I001]に対応する値。外部から読み込みができない場合には、定義書の値を戻します。必ずnull以外の値が戻ります。
     */
    public String getI001(final String arg0) {
        // 初期値として定義書の値を利用します。
        String strFormat = "こんにちは {0}";
        try {
            if (fResourceBundle != null) {
                strFormat = fResourceBundle.getString("I001");
            }
        } catch (MissingResourceException ex) {
        }
        final MessageFormat messageFormat = new MessageFormat(strFormat);
        final StringBuffer strbuf = new StringBuffer();
        // 与えられた引数を元に置換文字列を置き換えます。
        messageFormat.format(new Object[] {arg0}, strbuf, null);
        return strbuf.toString();
    }
}
