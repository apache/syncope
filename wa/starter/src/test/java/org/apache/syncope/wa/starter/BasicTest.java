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
package org.apache.syncope.wa.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

public class BasicTest extends AbstractTest {

    private String getLoginURL() {
        return "http://localhost:" + port + "/syncope-wa/login";
    }

    @Test
    public void loginLogout() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. first GET to fetch execution
        HttpGet get = new HttpGet(getLoginURL());
        get.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        CloseableHttpResponse response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());
        int begin = responseBody.indexOf("name=\"execution\" value=\"");
        assertNotEquals(-1, begin);
        int end = responseBody.indexOf("\"/><input type=\"hidden\" name=\"_eventId\"");
        assertNotEquals(-1, end);

        String execution = responseBody.substring(begin + 24, end);
        assertNotNull(execution);

        // 2. then POST to authenticate
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("_eventId", "submit"));
        form.add(new BasicNameValuePair("execution", execution));
        form.add(new BasicNameValuePair("username", "mrossi"));
        form.add(new BasicNameValuePair("password", "password"));
        form.add(new BasicNameValuePair("geolocation", ""));

        HttpPost post = new HttpPost(getLoginURL());
        post.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
        response = httpclient.execute(post, context);

        // 3. check authentication results
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Header[] cookie = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        assertTrue(cookie.length > 0);
        assertEquals(1, Stream.of(cookie).filter(item -> item.getValue().startsWith("TGC")).count());

        String body = EntityUtils.toString(response.getEntity());
        assertTrue(body.contains("Log In Successful"));
        assertTrue(body.contains("have successfully logged into the Central Authentication Service"));

        // 4. logout
        HttpGet logout = new HttpGet(getLoginURL().replace("login", "logout"));
        logout.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        response = httpclient.execute(logout, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        body = EntityUtils.toString(response.getEntity());
        assertTrue(body.contains("Logout successful"));
        assertTrue(body.contains("have successfully logged out of the Central Authentication Service"));
    }

    @Test
    public void loginError() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. first GET to fetch execution
        HttpGet get = new HttpGet(getLoginURL());
        get.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        CloseableHttpResponse response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseBody = EntityUtils.toString(response.getEntity());
        int begin = responseBody.indexOf("name=\"execution\" value=\"");
        assertNotEquals(-1, begin);
        int end = responseBody.indexOf("\"/><input type=\"hidden\" name=\"_eventId\"");
        assertNotEquals(-1, end);

        String execution = responseBody.substring(begin + 24, end);
        assertNotNull(execution);

        // 2. then POST to authenticate
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("_eventId", "submit"));
        form.add(new BasicNameValuePair("execution", execution));
        form.add(new BasicNameValuePair("username", "mrossi"));
        form.add(new BasicNameValuePair("password", "WRONG"));
        form.add(new BasicNameValuePair("geolocation", ""));

        HttpPost post = new HttpPost(getLoginURL());
        post.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
        response = httpclient.execute(post, context);

        // 3. check authentication results
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
    }
}
