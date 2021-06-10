package sls.http.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 通过 HTTP 协议往 Aliyun SLS 服务器上传日志
 * <p></p>
 * <p>
 * 参考文档：
 * <li>https://help.aliyun.com/document_detail/29007.html 概览 </li>
 * <li>https://help.aliyun.com/document_detail/29008.html 服务入口 </li>
 * <li>https://help.aliyun.com/document_detail/29026.htm PutLogs </li>
 * <li>https://help.aliyun.com/document_detail/29055.html 数据编码方式 </li>
 * <li>https://www.alibabacloud.com/help/zh/doc-detail/29012.htm 请求签名 </li>
 * </p>
 *
 * @author chiclaim@google.com
 */
public class HttpUploadSls {

    private static final String COMPRESS_TYPE = "deflate";
    private static final String SIGNATURE_METHOD = "hmac-sha1";
    private static final String API_VERSION = "0.6.0";
    private static final String CONTENT_TYPE = "application/x-protobuf";

    // 不要写死 SLS 配置信息，为了安全建议从自己的服务器上获取阿里云临时的 SLS Token
    private static final String LOG_STORE = "";
    private static final String HOST = "";
    private static final String ACCESS_KEY_SECRET = "";
    private static final String ACCESS_KEY_ID = "";
    private static final String SECURITY_TOKEN = "";

    // 负载均衡方式的 URL
    private static final String ADDRESS = "http://" + HOST + "/logstores/" + LOG_STORE + "/shards/lb";
    private static final String METHOD = "POST";


    public static String getDate() {
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date());
    }

    private static String getAuthorization(String signString) {
        String sign = getDigitalSignature(signString);
        return "LOG " + ACCESS_KEY_ID + ":" + sign;
    }

    private static String getSignString(String securityToken, String contentMD5, String contentType,
                                        String date, long bodyRawSize) {
        return METHOD + "\n" +
                contentMD5 + "\n" +
                contentType + "\n" +
                date + "\n" +

                // CanonicalizedLOGHeaders
                // key 按照升序
                "x-acs-security-token:" + securityToken + "\n" +
                "x-log-apiversion:" + API_VERSION + "\n" +
                "x-log-bodyrawsize:" + bodyRawSize + "\n" +
                "x-log-compresstype:" + COMPRESS_TYPE + "\n" +
                "x-log-signaturemethod:" + SIGNATURE_METHOD + "\n" +

                // CanonicalizedResource
                "/logstores/" + LOG_STORE + "/shards/lb";

    }

    /**
     * 生成数字签名
     * <p>
     * Signature = base64(hmac-sha1(UTF8-Encoding-Of(SignString)，AccessKeySecret))
     *
     * @see #getSignString(String, String, String, String, long)
     */
    private static String getDigitalSignature(String signString) {
        try {
            return HmacSha1Signature.hmacAndBase64(signString, ACCESS_KEY_SECRET);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static SlsLog.LogGroup addLog(Map<String, String> logs) {
        SlsLog.Log.Builder logBuilder = SlsLog.Log.newBuilder();
        logBuilder.setTime((int) (System.currentTimeMillis() / 1000L));
        for (Map.Entry<String, String> entry : logs.entrySet()) {
            logBuilder.addContents(SlsLog.Log.Content.newBuilder()
                    .setKey(entry.getKey()).setValue(entry.getValue()).build());
        }
        SlsLog.Log log = logBuilder.build();

        SlsLog.LogTag.Builder tagBuilder = SlsLog.LogTag.newBuilder();
        SlsLog.LogTag logTag = tagBuilder.setKey("__platform__").setValue("harmony").build();


        SlsLog.LogGroup.Builder logGroupBuilder = SlsLog.LogGroup.newBuilder()
                //.setSource()
                //.setTopic("harmony")
                .addLogTags(logTag)
                .addLogs(log);
        return logGroupBuilder.build();
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("harmonyTestKey", "harmonyTestValue");
        map.put("package", "com.zmsoft.nezha");
        uploadLog(map);
    }

    public static void uploadLog(Map<String, String> map) {

        OutputStream os = null;
        try {
            URL url = new URL(ADDRESS);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(METHOD);

            SlsLog.LogGroup logGroup = addLog(map);
            byte[] rawContent = logGroup.toByteArray();
            byte[] compressContent = ZLibUtils.compress(rawContent);
            final String contentMD5 = MD5.md5(compressContent);
            final int contentLength = compressContent.length;
            final int rawBodyLength = rawContent.length;

            final String date = getDate();

            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            // Content-Length、Content-MD5头部也是按照压缩后的Body部分计算
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            // Content-MD5头部也是按照压缩后的Body部分计算
            connection.setRequestProperty("Content-MD5", contentMD5);
            // 格式化字符串如下：%a,%d%b%Y %H:%M:%S GMT （如：Mon, 03 Jan 2010 08:33:47 GMT）
            connection.setRequestProperty("Date", date);
            connection.setRequestProperty("Host", HOST);
            connection.setRequestProperty("x-log-apiversion", API_VERSION);
            // 当Body是压缩数据，则为压缩前的原始数据大小
            connection.setRequestProperty("x-log-bodyrawsize", String.valueOf(rawBodyLength));
            // API 请求中 Body 部分使用的压缩方式。目前支持 lz4 压缩类型和 deflate 压缩类型
            connection.setRequestProperty("x-log-compresstype", COMPRESS_TYPE);
            connection.setRequestProperty("x-log-signaturemethod", SIGNATURE_METHOD);

            final String sign = getSignString(SECURITY_TOKEN, contentMD5, CONTENT_TYPE, date, rawBodyLength);
            final String auth = getAuthorization(sign);
            System.out.println(sign);
            System.out.println(auth);
            connection.setRequestProperty("Authorization", auth);
            connection.setRequestProperty("x-acs-security-token", SECURITY_TOKEN);

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            os = connection.getOutputStream();
            os.write(compressContent);
            os.flush();
            if (connection.getResponseCode() == 200) {
                System.out.println("日志上传成功");
            } else {
                InputStream in = connection.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String err = reader.readLine();
                reader.close();
                // 格式： {"errorCode":"Unauthorized","errorMessage":"The security token you provided has expired"}
                System.err.println("日志上传失败, code:" + connection.getResponseCode() + " ,err:" + err);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
