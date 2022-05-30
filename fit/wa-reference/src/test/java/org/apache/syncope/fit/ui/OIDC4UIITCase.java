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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;

public class OIDC4UIITCase extends AbstractUIITCase {

    private static void clientAppSetup(final String appName, final String baseAddress, final long appId) {
        OIDCRPClientAppTO clientApp = clientAppService.list(ClientAppType.OIDCRP).stream().
                filter(app -> appName.equals(app.getName())).
                map(OIDCRPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    OIDCRPClientAppTO app = new OIDCRPClientAppTO();
                    app.setName(appName);
                    app.setClientAppId(appId);
                    app.setClientId(appName);
                    app.setClientSecret(appName);

                    Response response = clientAppService.create(ClientAppType.OIDCRP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create OIDC Client App");
                    }

                    return clientAppService.read(
                            ClientAppType.OIDCRP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setClientId(appName);
        clientApp.setClientSecret(appName);
        clientApp.setSubjectType(OIDCSubjectType.PUBLIC);
        clientApp.getRedirectUris().clear();
        clientApp.getRedirectUris().add(baseAddress + OIDCC4UIConstants.URL_CONTEXT + "/code-consumer");
        clientApp.setAuthPolicy(getAuthPolicy().getKey());
        clientApp.setSignIdToken(true);
        clientApp.setJwtAccessToken(true);
        clientApp.setLogoutUri(baseAddress + OIDCC4UIConstants.URL_CONTEXT + "/logout");

        clientAppService.update(ClientAppType.OIDCRP, clientApp);
        clientAppService.pushToWA();
    }

    private static String getAppName(final String address) {
        return CONSOLE_ADDRESS.equals(address)
                ? OIDC4UIITCase.class.getName() + "_Console"
                : OIDC4UIITCase.class.getName() + "_Enduser";
    }

    @BeforeAll
    public static void consoleClientAppSetup() {
        clientAppSetup(getAppName(CONSOLE_ADDRESS), CONSOLE_ADDRESS, 7L);
    }

    @BeforeAll
    public static void enduserClientAppSetup() {
        clientAppSetup(getAppName(ENDUSER_ADDRESS), ENDUSER_ADDRESS, 8L);
    }

    private static void oidcSetup(
            final String appName,
            final boolean createUnmatching,
            final boolean selfRegUnmatching) {

        Optional<OIDCC4UIProviderTO> ops = oidcProviderService.list().stream().
                filter(op -> op.getName().equals(appName)).findFirst();
        if (ops.isEmpty()) {
            OIDCC4UIProviderTO cas = new OIDCC4UIProviderTO();
            cas.setName(appName);

            cas.setClientID(appName);
            cas.setClientSecret(appName);

            cas.setIssuer(WA_ADDRESS + "/oidc");
            cas.setAuthorizationEndpoint(cas.getIssuer() + "/authorize");
            cas.setTokenEndpoint(cas.getIssuer() + "/accessToken");
            cas.setJwksUri(cas.getIssuer() + "/jwks");
            cas.setUserinfoEndpoint(cas.getIssuer() + "/profile");
            cas.setEndSessionEndpoint(cas.getIssuer() + "/logout");

            cas.setCreateUnmatching(createUnmatching);
            cas.setSelfRegUnmatching(selfRegUnmatching);

            ItemTO item = new ItemTO();
            item.setIntAttrName("username");
            item.setExtAttrName("preferred_username");
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

            oidcProviderService.create(cas);
        }
    }

    @BeforeAll
    public static void consoleOIDCSetup() {
        oidcSetup(getAppName(CONSOLE_ADDRESS), true, false);
    }

    @BeforeAll
    public static void enduserOIDCSetup() {
        oidcSetup(getAppName(ENDUSER_ADDRESS), false, true);
    }

    @Override
    protected void sso(final String baseURL, final String username, final String password) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. fetch login page
        HttpGet get = new HttpGet(baseURL);
        CloseableHttpResponse response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // 2. click on the OpenID Connect Provider
        get = new HttpGet(baseURL + OIDCC4UIConstants.URL_CONTEXT + "/login?op=" + getAppName(baseURL));
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // 2a. redirected to WA login screen
        String responseBody = EntityUtils.toString(response.getEntity());
        response = authenticateToWA(username, password, responseBody, httpclient, context);

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

        // 2c. WA scope consent screen
        get = new HttpGet(response.getLastHeader(HttpHeaders.LOCATION).getValue());
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        responseBody = EntityUtils.toString(response.getEntity());

        String allow = Jsoup.parse(responseBody).body().
                getElementsByTag("a").select("a[name=allow]").first().
                attr("href");
        assertNotNull(allow);

        // 2d. finally get requested content
        get = new HttpGet(allow);
        get.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);

        // 3. verify that user is now authenticated
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).contains(username));
    }

    @Override
    protected void doSelfReg(final Runnable runnable) {
        runnable.run();
    }
}
