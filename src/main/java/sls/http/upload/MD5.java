package sls.http.upload;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by chiclaim@google.com
 */
public class MD5 {

    private static final int LO_BYTE = 0x0f;
    private static final int MOVE_BIT = 4;
    private static final int HI_BYTE = 0xf0;
    private static final String[] HEX_DIGITS = { "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    private MD5() {}

    /**
     * 转换字节数组为16进制字串
     *
     * @param b 字节数组
     * @return 16进制字串
     */

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder buf = new StringBuilder();
        for (byte value : b) {
            buf.append(byteToHexString(value));
            // 也可以使用下面的方式。 X 表示大小字母，x 表示小写字母，对应的是 HEX_DIGITS 中字母
            // buf.append(String.format("%02X", value));
        }
        return buf.toString();
    }

    /**
     * 字节转成字符.
     *
     * @param b 原始字节.
     * @return 转换后的字符.
     */
    private static String byteToHexString(byte b) {
        return HEX_DIGITS[(b & HI_BYTE) >> MOVE_BIT] + HEX_DIGITS[b & LO_BYTE];
    }

    /**
     * 进行加密.
     *
     * @return 加密后的结果.
     */
    public static String md5(byte[] origin) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return byteArrayToHexString(md.digest(origin));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String md5(String str){
        return md5(str.getBytes());
    }


    /**
     * 对输入流生成校验码.
     * @param in 输入流.
     * @return 生成的校验码.
     */
    public static String md5(InputStream in) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }

            return byteArrayToHexString(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
           e.printStackTrace();
        }
        return null;
    }

}

