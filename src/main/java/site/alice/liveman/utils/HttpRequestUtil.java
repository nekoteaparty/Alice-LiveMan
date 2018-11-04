/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package site.alice.liveman.utils;

import lombok.Cleanup;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

public class HttpRequestUtil {

    public static String downloadUrl(URL url, Charset charset, Proxy proxy) throws IOException {
        return downloadUrl(url, null, Collections.emptyMap(), charset, proxy);
    }

    public static String downloadUrl(URL url, String cookies, Map<String, String> requestProperties, Charset charset, Proxy proxy) throws IOException {
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
        if (requestProperties != null) {
            for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        if (StringUtils.containsIgnoreCase(conn.getHeaderField("Content-Encoding"), "gzip")) {
            is = new GZIPInputStream(is);
        }
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
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        conn.connect();
        conn.getOutputStream().write(postData.getBytes(charset));
        InputStream is = conn.getInputStream();
        if (StringUtils.containsIgnoreCase(conn.getHeaderField("Content-Encoding"), "gzip")) {
            is = new GZIPInputStream(is);
        }
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
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        if (StringUtils.containsIgnoreCase(conn.getHeaderField("Content-Encoding"), "gzip")) {
            is = new GZIPInputStream(is);
        }
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
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        conn.connect();
        InputStream is = conn.getInputStream();
        if (StringUtils.containsIgnoreCase(conn.getHeaderField("Content-Encoding"), "gzip")) {
            is = new GZIPInputStream(is);
        }
        File tempFile = new File(file.toString() + ".tmp");
        tempFile.deleteOnExit();
        tempFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int readCount = 0;
            while ((readCount = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readCount);
            }
            is.close();
        }
        tempFile.renameTo(file);
    }
}
