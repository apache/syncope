/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.installer.utilities;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpUtils {

    private static final String HTTPS_URL_TEMPLATE = "https://%s:%s";

    private static final String HTTP_URL_TEMPLATE = "http://%s:%s";

    private final CloseableHttpClient httpClient;

    private final boolean isSsl;

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    private final HttpHost targetHost;

    public HttpUtils(final boolean isSsl, final String host,
            final String port, final String username, final String password) {

        this.isSsl = isSsl;
        this.host = host;
        this.port = Integer.valueOf(port);

        if (isSsl) {
            httpClient = createHttpsClient();
            this.targetHost = new HttpHost(this.host, this.port, "https");
        } else {
            httpClient = HttpClients.createDefault();
            this.targetHost = new HttpHost(this.host, this.port, "http");
        }
        
        this.username = username;
        this.password = password;
    }

    public int getWithBasicAuth(final String path) {
        final HttpGet httpGet;
        if (isSsl) {
            httpGet = new HttpGet(String.format(HTTPS_URL_TEMPLATE, host, port) + path);
        } else {
            httpGet = new HttpGet(String.format(HTTP_URL_TEMPLATE, host, port) + path);
        }
        int status = 0;
        try {
            final CloseableHttpResponse response = httpClient.execute(
                    targetHost, httpGet, setAuth(targetHost, new BasicScheme()));
            status = response.getStatusLine().getStatusCode();
            response.close();
        } catch (IOException ex) {
        }
        return status;
    }

    public String postWithDigestAuth(final String url, final String file) {
        String responseBodyAsString = "";
        try {
            final CloseableHttpResponse response = httpClient.execute(targetHost,
                    httpPost(url, MultipartEntityBuilder.create().addPart("bin", new FileBody(new File(file))).build()),
                    setAuth(targetHost, new DigestScheme()));
            responseBodyAsString = IOUtils.toString(response.getEntity().getContent());
            response.close();
        } catch (IOException ex) {
        }

        return responseBodyAsString;
    }

    public int postWithStringEntity(final String url, final String stringEntity) {
        int status = 0;
        try {
            final HttpPost httPost = httpPost(url, new StringEntity(stringEntity));
            httPost.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            final CloseableHttpResponse response = httpClient.execute(
                    targetHost, httPost, setAuth(targetHost, new DigestScheme()));
            status = response.getStatusLine().getStatusCode();
            response.close();
        } catch (IOException ioe) {
        }
        return status;
    }

    private HttpClientContext setAuth(final HttpHost targetHost, final AuthScheme authScheme) {
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(username, password));
        final HttpClientContext context = HttpClientContext.create();
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, authScheme);
        context.setAuthCache(authCache);
        context.setCredentialsProvider(credsProvider);
        return context;
    }

    private HttpPost httpPost(final String url, final HttpEntity reqEntity) {
        final HttpPost httppost = new HttpPost(url);
        httppost.setEntity(reqEntity);
        return httppost;
    }

    public static int ping(final boolean isSsl, final String host, final String port) {
        int status = 0;
        try {
            if (isSsl) {
                status = createHttpsClient().execute(
                        new HttpGet(String.format(HTTPS_URL_TEMPLATE, host, port))).getStatusLine().
                        getStatusCode();
            } else {
                status = HttpClients.createDefault().execute(
                        new HttpGet(String.format(HTTP_URL_TEMPLATE, host, port))).getStatusLine().getStatusCode();
            }
        } catch (IOException ex) {
        }

        return status;
    }

    private static CloseableHttpClient createHttpsClient() {
        CloseableHttpClient chc = null;
        try {
            final SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            chc = HttpClients.custom().setSSLSocketFactory(
                    new SSLConnectionSocketFactory(builder.build(),
                            SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)).build();
        } catch (KeyManagementException ex) {
        } catch (NoSuchAlgorithmException ex) {
        } catch (KeyStoreException ex) {
        }
        return chc;
    }
}
