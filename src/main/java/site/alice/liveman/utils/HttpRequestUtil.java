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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class HttpRequestUtil {

    private static PoolingHttpClientConnectionManager connectionManager;
    private static CloseableHttpClient                client;

    static {
        initClient();
    }

    private static synchronized void initClient() {
        if (client != null) {
            try {
                client.close();
                connectionManager.shutdown();
            } catch (IOException ignore) {

            }
        }
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new ProxyConnectionSocketFactory())
                .register("https", new ProxySSLConnectionSocketFactory(SSLContexts.createSystemDefault())).build();
        connectionManager = new PoolingHttpClientConnectionManager(reg, null, null, null, 5, TimeUnit.MINUTES);
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        client = HttpClients.custom().setConnectionManager(connectionManager).setConnectionManagerShared(true).build();
    }

    public static String downloadUrl(URI url, Charset charset, Proxy proxy) throws IOException {
        return downloadUrl(url, null, Collections.emptyMap(), charset, proxy);
    }

    public static String downloadUrl(URI url, String cookies, Map<String, String> requestProperties, Charset charset, Proxy proxy) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("proxy", proxy);
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(2000).setConnectionRequestTimeout(2000).setSocketTimeout(5000).setCookieSpec(CookieSpecs.IGNORE_COOKIES).setRedirectsEnabled(true);
        httpGet.setConfig(builder.build());
        if (StringUtils.isNotBlank(cookies)) {
            httpGet.addHeader("Cookie", cookies);
        }
        if (requestProperties != null) {
            for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
        }
        httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.addHeader("Accept-Encoding", "gzip, deflate");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        try (CloseableHttpResponse httpResponse = client.execute(httpGet, context)) {
            HttpEntity responseEntity = httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(responseEntity);
                throw new IOException(httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());
            }
            return EntityUtils.toString(responseEntity, charset);
        } catch (IllegalStateException e) {
            initClient();
            throw e;
        }
    }

    public static String downloadUrl(URI url, String cookies, String postData, Charset charset, Proxy proxy) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("proxy", proxy);
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(2000).setConnectionRequestTimeout(2000).setSocketTimeout(5000).setCookieSpec(CookieSpecs.IGNORE_COOKIES).setRedirectsEnabled(true);
        httpPost.setConfig(builder.build());
        if (StringUtils.isNotBlank(cookies)) {
            httpPost.addHeader("Cookie", cookies);
        }
        httpPost.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpPost.addHeader("Accept-Encoding", "gzip, deflate");
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        if (postData.startsWith("{")) {
            httpPost.setEntity(new StringEntity(postData, ContentType.APPLICATION_JSON));
        } else {
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            String[] formItems = postData.split("&");
            for (String formItem : formItems) {
                String[] itemData = formItem.split("=");
                nameValuePairs.add(new BasicNameValuePair(itemData[0], itemData.length > 1 ? itemData[1] : StringUtils.EMPTY));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, charset));
        }
        try (CloseableHttpResponse httpResponse = client.execute(httpPost, context)) {
            HttpEntity responseEntity = httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(responseEntity);
                throw new IOException(httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());
            }
            return EntityUtils.toString(responseEntity, charset);
        } catch (IllegalStateException e) {
            initClient();
            throw e;
        }
    }

    public static byte[] downloadUrl(URI url, Proxy proxy) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("proxy", proxy);
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(2000).setConnectionRequestTimeout(2000).setSocketTimeout(5000).setCookieSpec(CookieSpecs.IGNORE_COOKIES).setRedirectsEnabled(true);
        httpGet.setConfig(builder.build());
        httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.addHeader("Accept-Encoding", "gzip, deflate");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        try (CloseableHttpResponse httpResponse = client.execute(httpGet, context)) {
            HttpEntity responseEntity = httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(responseEntity);
                throw new IOException(httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());
            }
            return EntityUtils.toByteArray(responseEntity);
        } catch (IllegalStateException e) {
            initClient();
            throw e;
        }
    }

    public static void downloadToFile(URI url, File file, Proxy proxy) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("proxy", proxy);
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(2000).setConnectionRequestTimeout(2000).setSocketTimeout(5000).setCookieSpec(CookieSpecs.IGNORE_COOKIES).setRedirectsEnabled(true);
        httpGet.setConfig(builder.build());
        httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.addHeader("Accept-Encoding", "gzip, deflate");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        try (CloseableHttpResponse httpResponse = client.execute(httpGet, context)) {
            HttpEntity responseEntity = httpResponse.getEntity();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(responseEntity);
                throw new IOException(httpResponse.getStatusLine().getStatusCode() + " " + httpResponse.getStatusLine().getReasonPhrase());
            }
            InputStream is = responseEntity.getContent();
            if (responseEntity.getContentEncoding() != null && StringUtils.containsIgnoreCase(responseEntity.getContentEncoding().getValue(), "gzip")) {
                is = new GZIPInputStream(is);
            }
            File tempFile = new File(file.toString() + ".tmp");
            tempFile.deleteOnExit();
            tempFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int readCount = -1;
                while ((readCount = is.read(buffer)) > -1) {
                    fos.write(buffer, 0, readCount);
                }
                is.close();
            }
            tempFile.renameTo(file);
            EntityUtils.consume(responseEntity);
        } catch (IllegalStateException e) {
            initClient();
            throw e;
        }
    }

    static class ProxySSLConnectionSocketFactory extends SSLConnectionSocketFactory {

        public ProxySSLConnectionSocketFactory(SSLContext sslContext) {
            super(sslContext);
        }

        @Override
        public Socket createSocket(final HttpContext context) {
            Proxy proxy = (Proxy) context.getAttribute("proxy");
            if (proxy == null) {
                return new Socket();
            } else {
                return new Socket(proxy);
            }
        }

        @Override
        public Socket connectSocket(
                final int connectTimeout,
                final Socket socket,
                final HttpHost host,
                final InetSocketAddress remoteAddress,
                final InetSocketAddress localAddress,
                final HttpContext context) throws IOException {
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }

    static class ProxyConnectionSocketFactory extends PlainConnectionSocketFactory {

        @Override
        public Socket createSocket(final HttpContext context) {
            Proxy proxy = (Proxy) context.getAttribute("proxy");
            if (proxy == null) {
                return new Socket();
            } else {
                return new Socket(proxy);
            }
        }

        @Override
        public Socket connectSocket(
                final int connectTimeout,
                final Socket socket,
                final HttpHost host,
                final InetSocketAddress remoteAddress,
                final InetSocketAddress localAddress,
                final HttpContext context) throws IOException {
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }
}
