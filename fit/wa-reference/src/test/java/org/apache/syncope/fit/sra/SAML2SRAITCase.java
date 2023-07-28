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
import org.apache.commons.lang3.tuple.Triple;
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
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SAML2SRAITCase extends AbstractSRAITCase {

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(SAML2SRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("saml2");
    }

    @BeforeAll
    public static void clientAppSetup() {
        String appName = SAML2SRAITCase.class.getName();
        SAML2SPClientAppTO clientApp = CLIENT_APP_SERVICE.list(ClientAppType.SAML2SP).stream().
                filter(app -> appName.equals(app.getName())).
                map(SAML2SPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    SAML2SPClientAppTO app = new SAML2SPClientAppTO();
                    app.setName(appName);
                    app.setRealm(SyncopeConstants.ROOT_REALM);
                    app.setClientAppId(3L);
                    app.setEntityId(SRA_ADDRESS);
                    app.setMetadataLocation(SRA_ADDRESS + "/saml2/metadata");

                    Response response = CLIENT_APP_SERVICE.create(ClientAppType.SAML2SP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create SAML2 Client App");
                    }

                    return CLIENT_APP_SERVICE.read(
                            ClientAppType.SAML2SP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setSignAssertions(true);
        clientApp.setSignResponses(true);
        clientApp.setRequiredNameIdFormat(SAML2SPNameId.PERSISTENT);
        clientApp.setAuthPolicy(getAuthPolicy().getKey());

        CLIENT_APP_SERVICE.update(ClientAppType.SAML2SP, clientApp);
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
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            ObjectNode headers = checkGetResponse(response, get.getURI().toASCIIString().replace("/public", ""));
            assertFalse(headers.has(HttpHeaders.COOKIE));
        }

        // 2. protected
        get = new HttpGet(SRA_ADDRESS + "/protected/get?" + QUERY_STRING);
        String originalRequestURI = get.getURI().toASCIIString();
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        String responseBody;
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            responseBody = EntityUtils.toString(response.getEntity());
        }

        // 2a. post SAML request
        Triple<String, String, String> parsed = parseSAMLRequestForm(responseBody);

        HttpPost post = new HttpPost(parsed.getLeft());
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        post.setEntity(new UrlEncodedFormEntity(
                List.of(new BasicNameValuePair("RelayState", parsed.getMiddle()),
                        new BasicNameValuePair("SAMLRequest", parsed.getRight())), Consts.UTF_8));
        String location;
        try (CloseableHttpResponse response = httpclient.execute(post, context)) {
            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
            location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
        }

        // 2b. authenticate
        post = new HttpPost(location);
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        try (CloseableHttpResponse response = httpclient.execute(post, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            responseBody = EntityUtils.toString(response.getEntity());
        }

        boolean isOk = false;
        try (CloseableHttpResponse response =
                authenticateToWA("bellini", "password", responseBody, httpclient, context)) {

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    isOk = true;
                    responseBody = EntityUtils.toString(response.getEntity());
                    break;

                case HttpStatus.SC_MOVED_TEMPORARILY:
                    location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                    break;

                default:
                    fail();
            }
        }

        // 2c. WA attribute consent screen
        if (isOk) {
            String execution = extractWAExecution(responseBody);

            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("_eventId", "confirm"));
            form.add(new BasicNameValuePair("execution", execution));
            form.add(new BasicNameValuePair("option", "1"));
            form.add(new BasicNameValuePair("reminder", "30"));
            form.add(new BasicNameValuePair("reminderTimeUnit", "days"));

            post = new HttpPost(WA_ADDRESS + "/login");
            post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
            post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
            post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(post, context)) {
                assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
                location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
            }
        }

        get = new HttpGet(location);
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            responseBody = EntityUtils.toString(response.getEntity());
        }

        // 2d. post SAML response
        parsed = parseSAMLResponseForm(responseBody);

        post = new HttpPost(parsed.getLeft());
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        post.setEntity(new UrlEncodedFormEntity(
                List.of(new BasicNameValuePair("RelayState", parsed.getMiddle()),
                        new BasicNameValuePair("SAMLResponse", parsed.getRight())), Consts.UTF_8));
        try (CloseableHttpResponse response = httpclient.execute(post, context)) {
            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
            location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
        }

        // 2e. finally get requested content
        get = new HttpGet(location);
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            ObjectNode headers = checkGetResponse(response, originalRequestURI.replace("/protected", ""));
            assertFalse(headers.get(HttpHeaders.COOKIE).asText().isBlank());
        }

        // 3. logout
        get = new HttpGet(SRA_ADDRESS + "/protected/logout");
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            responseBody = EntityUtils.toString(response.getEntity());
        }

        // 3a. post SAML request
        parsed = parseSAMLRequestForm(responseBody);

        post = new HttpPost(parsed.getLeft());
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        post.setEntity(new UrlEncodedFormEntity(
                List.of(new BasicNameValuePair("RelayState", parsed.getMiddle()),
                        new BasicNameValuePair("SAMLRequest", parsed.getRight())), Consts.UTF_8));
        try (CloseableHttpResponse response = httpclient.execute(post, context)) {
            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
            location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
        }

        get = new HttpGet(location);
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        // 3b. check logout
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            checkLogout(response);
        }
    }
}
