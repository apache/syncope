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
package org.apache.syncope.fit.sra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.http.Consts;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CASSRAITCase extends AbstractSRAITCase {

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(CASSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("cas");
    }

    @BeforeAll
    public static void clientAppSetup() {
        String appName = CASSRAITCase.class.getName();
        CASSPClientAppTO clientApp = CLIENT_APP_SERVICE.list(ClientAppType.CASSP).stream().
                filter(app -> appName.equals(app.getName())).
                map(CASSPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    CASSPClientAppTO app = new CASSPClientAppTO();
                    app.setName(appName);
                    app.setRealm(SyncopeConstants.ROOT_REALM);
                    app.setClientAppId(4L);
                    app.setServiceId("http://127.0.0.1:8080/.*");

                    Response response = CLIENT_APP_SERVICE.create(ClientAppType.CASSP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create CAS Client App");
                    }

                    return CLIENT_APP_SERVICE.read(
                            ClientAppType.CASSP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setAuthPolicy(getAuthPolicy().getKey());

        CLIENT_APP_SERVICE.update(ClientAppType.CASSP, clientApp);
        WA_CONFIG_SERVICE.pushToWA(WAConfigService.PushSubject.clientApps, List.of());
    }

    @Test
    public void web() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. public
        HttpGet get = new HttpGet(SRA_ADDRESS + "/public/get?" + QUERY_STRING);
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        CloseableHttpResponse response = httpclient.execute(get, context);

        ObjectNode headers = checkGetResponse(response, get.getURI().toASCIIString().replace("/public", ""));
        assertFalse(headers.has(HttpHeaders.COOKIE));

        // 2. protected
        get = new HttpGet(SRA_ADDRESS + "/protected/get?" + QUERY_STRING);
        String originalRequestURI = get.getURI().toASCIIString();
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // 2a. authenticate
        String responseBody = EntityUtils.toString(response.getEntity());
        response = authenticateToWA("bellini", "password", responseBody, httpclient, context);

        // 2b. WA attribute consent screen
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            responseBody = EntityUtils.toString(response.getEntity());
            String execution = extractWAExecution(responseBody);

            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("_eventId", "confirm"));
            form.add(new BasicNameValuePair("execution", execution));
            form.add(new BasicNameValuePair("option", "1"));
            form.add(new BasicNameValuePair("reminder", "30"));
            form.add(new BasicNameValuePair("reminderTimeUnit", "days"));

            HttpPost post = new HttpPost(WA_ADDRESS + "/login");
            post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
            post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
            post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
            response = httpclient.execute(post, context);
        }
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());

        // 2c. finally get requested content
        get = new HttpGet(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);

        headers = checkGetResponse(response, originalRequestURI.replace("/protected", ""));
        assertFalse(headers.get(HttpHeaders.COOKIE).asText().isBlank());

        // 3. logout
        get = new HttpGet(SRA_ADDRESS + "/protected/logout");
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);

        checkLogout(response);
    }
}
