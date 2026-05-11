package com.jiahao.food.sdk.http;

import com.jiahao.food.sdk.config.ClientConfig;
import com.jiahao.food.sdk.exception.NetworkException;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

/**
 * SDK HTTP 客户端（连接池）。
 */
public class SdkHttpClient {

    private final CloseableHttpClient httpClient;

    public SdkHttpClient(ClientConfig config) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(10);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(config.getConnectTimeout())
                        .setSocketTimeout(config.getReadTimeout())
                        .build())
                .build();
    }

    public HttpResponse execute(String url, Map<String, String> headers) throws NetworkException {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity, "UTF-8") : "";
            return new HttpResponse(response.getStatusLine().getStatusCode(), body);
        } catch (IOException e) {
            throw new NetworkException("HTTP request failed: " + url, e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
