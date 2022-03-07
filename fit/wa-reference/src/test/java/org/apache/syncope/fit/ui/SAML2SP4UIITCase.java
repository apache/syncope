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
package org.apache.syncope.fit.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
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
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SAML2SP4UIITCase extends AbstractUIITCase {

    private static void clientAppSetup(final String appName, final String entityId, final long appId) {
        SAML2SPClientAppTO clientApp = clientAppService.list(ClientAppType.SAML2SP).stream().
                filter(app -> appName.equals(app.getName())).
                map(SAML2SPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    SAML2SPClientAppTO app = new SAML2SPClientAppTO();
                    app.setName(appName);
                    app.setClientAppId(appId);
                    app.setEntityId(entityId);
                    app.setMetadataLocation(entityId + SAML2SP4UIConstants.URL_CONTEXT + "/metadata");

                    Response response = clientAppService.create(ClientAppType.SAML2SP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create SAML2 Client App");
                    }

                    return clientAppService.read(
                            ClientAppType.SAML2SP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setSignAssertions(true);
        clientApp.setSignResponses(true);
        clientApp.setRequiredNameIdFormat(SAML2SPNameId.PERSISTENT);
        clientApp.setAuthPolicy(getAuthPolicy().getKey());

        clientAppService.update(ClientAppType.SAML2SP, clientApp);
        clientAppService.pushToWA();
    }

    @BeforeAll
    public static void consoleClientAppSetup() {
        clientAppSetup(SAML2SP4UIITCase.class.getName() + "_Console", CONSOLE_ADDRESS, 5L);
    }

    @BeforeAll
    public static void enduserClientAppSetup() {
        clientAppSetup(SAML2SP4UIITCase.class.getName() + "_Enduser", ENDUSER_ADDRESS, 6L);
    }

    @BeforeAll
    public static void idpSetup() {
        WebClient.client(saml2sp4UIIdPService).
                accept(MediaType.APPLICATION_XML_TYPE).
                type(MediaType.APPLICATION_XML_TYPE);
        try {
            saml2sp4UIIdPService.importFromMetadata(
                    (InputStream) WebClient.create(WA_ADDRESS + "/idp/metadata").get().getEntity());
        } catch (SyncopeClientException e) {
            // nothing bad if already imported
        } finally {
            WebClient.client(saml2sp4UIIdPService).
                    accept(clientFactory.getContentType().getMediaType()).
                    type(clientFactory.getContentType().getMediaType());
        }

        List<SAML2SP4UIIdPTO> idps = saml2sp4UIIdPService.list();
        assertEquals(1, idps.size());

        SAML2SP4UIIdPTO cas = idps.get(0);
        cas.setEntityID(WA_ADDRESS + "/saml");
        cas.setName("CAS");
        cas.setCreateUnmatching(true);
        cas.setSelfRegUnmatching(false);
        cas.getItems().clear();

        ItemTO item = new ItemTO();
        item.setIntAttrName("username");
        item.setExtAttrName("NameID");
        item.setConnObjectKey(true);
        cas.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setIntAttrName("email");
        item.setExtAttrName("mail");
        cas.add(item);

        item = new ItemTO();
        item.setIntAttrName("userId");
        item.setExtAttrName("mail");
        cas.add(item);

        item = new ItemTO();
        item.setIntAttrName("firstname");
        item.setExtAttrName("givenName");
        cas.add(item);

        item = new ItemTO();
        item.setIntAttrName("surname");
        item.setExtAttrName("sn");
        cas.add(item);

        item = new ItemTO();
        item.setIntAttrName("fullname");
        item.setExtAttrName("cn");
        cas.add(item);

        saml2sp4UIIdPService.update(cas);
    }

    @Test
    public void fetchSpMetadata() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(new BasicCookieStore());

            HttpGet get = new HttpGet(WA_ADDRESS + "/sp/metadata");
            CloseableHttpResponse response = httpclient.execute(get, context);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String responseBody = EntityUtils.toString(response.getEntity());
            assertFalse(responseBody.isEmpty());
        }
    }

    @Override
    protected void sso(final String baseURL, final String username, final String password) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. fetch login page
        HttpGet get = new HttpGet(baseURL);
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        }

        // 2. click on the SAML 2.0 IdP
        get = new HttpGet(baseURL + SAML2SP4UIConstants.URL_CONTEXT
                + "/login?idp=https%3A//localhost%3A9443/syncope-wa/saml");
        String responseBody;
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            responseBody = EntityUtils.toString(response.getEntity());
        }
        Triple<String, String, String> parsed = parseSAMLRequestForm(responseBody);

        // 2a. post SAML request
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
        post.addHeader(HttpHeaders.REFERER, get.getURI().toASCIIString());
        try (CloseableHttpResponse response = httpclient.execute(post, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            responseBody = EntityUtils.toString(response.getEntity());
        }
        boolean isOk = false;
        try (CloseableHttpResponse response =
                authenticateToWA(username, password, responseBody, httpclient, context)) {

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

        // 3. verify that user is now authenticated
        get = new HttpGet(baseURL + StringUtils.removeStart(location, "../"));
        try (CloseableHttpResponse response = httpclient.execute(get, context)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertTrue(EntityUtils.toString(response.getEntity()).contains(username));
        }
    }

    @Override
    protected void doSelfReg(final Runnable runnable) {
        List<SAML2SP4UIIdPTO> idps = saml2sp4UIIdPService.list();
        assertEquals(1, idps.size());

        SAML2SP4UIIdPTO cas = idps.get(0);
        cas.setCreateUnmatching(false);
        cas.setSelfRegUnmatching(true);
        saml2sp4UIIdPService.update(cas);

        try {
            runnable.run();
        } finally {
            cas.setCreateUnmatching(true);
            cas.setSelfRegUnmatching(false);
            saml2sp4UIIdPService.update(cas);
        }
    }
}
