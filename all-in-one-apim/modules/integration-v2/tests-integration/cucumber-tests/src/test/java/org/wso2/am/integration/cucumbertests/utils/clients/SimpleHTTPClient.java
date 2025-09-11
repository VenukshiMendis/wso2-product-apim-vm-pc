/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils.clients;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.wso2.am.integration.test.Constants;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class SimpleHTTPClient {

    private static final Log log = LogFactory.getLog(SimpleHTTPClient.class);
    private final CloseableHttpClient client;

    private SimpleHTTPClient()  {

        try {
            // Initialize SSL Context to trust all certificates
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
            connManager.setMaxTotal(1000);        // total max connections
            connManager.setDefaultMaxPerRoute(100);   // max connections per route

            RequestConfig requestConfig = RequestConfig.custom()
                    .setRedirectsEnabled(false) // Disable redirects
                    .build();

            this.client = HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setSSLSocketFactory(csf)
                    .setDefaultRequestConfig(requestConfig)
                    .evictExpiredConnections()
                    .build();

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SimpleHTTPClient with SSL context", e);
        }
    }

    private static class InstanceHolder  {
        private static final SimpleHTTPClient INSTANCE = new SimpleHTTPClient();
    }

    public static SimpleHTTPClient getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Send a HTTP GET request to the specified URL
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doGet(String url, Map<String, String> headers)
            throws IOException {

        log.info("GET endpoint url:" + url);
        HttpGet request = new HttpGet(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP DELETE request to the specified URL
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doDelete(
            String url, final Map<String, String> headers) throws IOException {

        HttpDelete request = new HttpDelete(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP POST request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPost(
            String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        log.info("POST endpoint url: " + url);
        HttpPost request = new HttpPost(url);
        setHeaders(headers, request);
        boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP PUT request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPut(
            String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        HttpPut request = new HttpPut(url);
        setHeaders(headers, request);
        final boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }


    /**
     * Send a HTTP PATCH request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPatch(String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        HttpPatch request = new HttpPatch(url);
        setHeaders(headers, request);
        final boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Builds an {@link EntityTemplate} that writes the given payload into the request body.
     *
     * @param payload     the request body content
     * @param contentType the MIME type of the request body (defaults to application/json if null)
     * @param zip         whether to gzip-compress the payload
     * @return configured EntityTemplate for use in an HTTP request
     */
    private static EntityTemplate getEntityTemplate(String payload, String contentType, boolean zip) {
        EntityTemplate ent = new EntityTemplate(outputStream -> {
            OutputStream out = zip ? new GZIPOutputStream(outputStream) : outputStream;
            try {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                if (zip) {
                    out.close();
                }
            }
        });

        ent.setContentType(contentType != null ? contentType : Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (zip) {
            ent.setContentEncoding("gzip");
        }
        return ent;
    }

    /**
     * Sets all headers from the given map onto the HTTP request.
     *
     * @param headers map of header names and values
     * @param request the request to update
     */
    private void setHeaders(Map<String, String> headers, HttpUriRequest request) {

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
    }

    /**
     * Construct the org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     *
     * @param response org.apache.http.HttpResponse
     * @return org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     * @throws IOException if any exception occurred when reading payload
     */
    private static org.wso2.carbon.automation.test.utils.http.client.HttpResponse constructResponse(
            HttpResponse response) throws IOException {

        int code = response.getStatusLine().getStatusCode();
        String body = responseEntityBodyToString(response);
        Header[] headers = response.getAllHeaders();
        Map<String, String> heads = new HashMap<>();
        for (Header header : headers) {
            heads.put(header.getName(), header.getValue());
        }
        return new org.wso2.carbon.automation.test.utils.http.client.HttpResponse(
                body, code, heads);
    }

    /**
     * read the response body as String
     *
     * @param response http response with type org.apache.http.HttpResponse
     * @return String of the response body
     * @throws IOException throws if any error occurred
     */
    public static String responseEntityBodyToString(HttpResponse response) throws IOException {
        if (response != null && response.getEntity() != null) {
            try (InputStream inputStreamContent = response.getEntity().getContent()) {
                return IOUtils.toString(inputStreamContent, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
