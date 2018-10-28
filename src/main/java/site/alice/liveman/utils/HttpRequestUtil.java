package site.alice.liveman.utils;

import lombok.Cleanup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class HttpRequestUtil {

    public static String downloadUrl(URL url, Charset charset, Proxy proxy) throws IOException {
        return downloadUrl(url, null, charset, proxy);
    }

    public static String downloadUrl(URL url, String cookies, Charset charset, Proxy proxy) throws IOException {
        URLConnection conn;
        if (proxy != null) {
            conn = url.openConnection(proxy);
        } else {
            conn = url.openConnection();
        }
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        if (StringUtils.isNotBlank(cookies)) {
            conn.setRequestProperty("Cookie", cookies);
        }
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while ((readCount = is.read(buffer)) > 0) {
            bos.write(buffer, 0, readCount);
        }
        is.close();
        return new String(bos.toByteArray(), charset);
    }

    public static String downloadUrl(URL url, String cookies, String postData, Charset charset, Proxy proxy) throws IOException {
        URLConnection conn;
        if (proxy != null) {
            conn = url.openConnection(proxy);
        } else {
            conn = url.openConnection();
        }
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        if (StringUtils.isNotBlank(cookies)) {
            conn.setRequestProperty("Cookie", cookies);
        }
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        conn.connect();
        conn.getOutputStream().write(postData.getBytes(charset));
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while ((readCount = is.read(buffer)) > 0) {
            bos.write(buffer, 0, readCount);
        }
        is.close();
        return new String(bos.toByteArray(), charset);
    }

    public static byte[] downloadUrl(URL url, Proxy proxy) throws IOException {
        URLConnection conn;
        if (proxy != null) {
            conn = url.openConnection(proxy);
        } else {
            conn = url.openConnection();
        }
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while ((readCount = is.read(buffer)) > 0) {
            bos.write(buffer, 0, readCount);
        }
        is.close();
        return bos.toByteArray();
    }

    public static void downloadToFile(URL url, File file, Proxy proxy) throws IOException {
        URLConnection conn;
        if (proxy != null) {
            conn = url.openConnection(proxy);
        } else {
            conn = url.openConnection();
        }
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        File tempFile = new File(file.toString() + ".tmp");
        tempFile.deleteOnExit();
        tempFile.getParentFile().mkdirs();
        @Cleanup FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while ((readCount = is.read(buffer)) > 0) {
            fos.write(buffer, 0, readCount);
        }
        is.close();
        fos.close();
        tempFile.renameTo(file);
    }
}
