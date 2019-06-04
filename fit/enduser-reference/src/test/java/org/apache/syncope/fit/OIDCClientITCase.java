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
package org.apache.syncope.fit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OIDCClientITCase extends AbstractITCase {

    @BeforeAll
    public static void samlSetup() {
        OIDCProviderTO keycloak = new OIDCProviderTO();
        keycloak.setName("Keyloack");
        keycloak.setClientID("oidc-syncope");
        keycloak.setClientSecret("oidc-syncope");
        keycloak.setAuthorizationEndpoint("http://localhost:9090/auth/realms/master/protocol/openid-connect/auth");
        keycloak.setTokenEndpoint("http://localhost:9090/auth/realms/master/protocol/openid-connect/token");
        keycloak.setIssuer("http://localhost:9090/auth/realms/master");
        keycloak.setJwksUri("http://localhost:9090/auth/realms/master/protocol/openid-connect/certs");
        keycloak.setUserinfoEndpoint("http://localhost:9090/auth/realms/master/protocol/openid-connect/userinfo");
        keycloak.setEndSessionEndpoint("http://localhost:9090/auth/realms/master/protocol/openid-connect/logout");
        keycloak.setCreateUnmatching(true);

        ItemTO item = new ItemTO();
        item.setIntAttrName("username");
        item.setExtAttrName("preferred_username");
        item.setConnObjectKey(true);
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("email");
        item.setExtAttrName("email");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("userId");
        item.setExtAttrName("email");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("firstname");
        item.setExtAttrName("given_name");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("surname");
        item.setExtAttrName("family_name");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("fullname");
        item.setExtAttrName("fullName");
        keycloak.getItems().add(item);

        oidcProviderService.create(keycloak);
    }

    private void sso(final String baseURL) throws IOException {
        CloseableHttpClient httpclient = HttpClients.custom().setMaxConnPerRoute(100).build();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. fetch login page
        HttpGet get = new HttpGet(baseURL);
        CloseableHttpResponse response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // 2. click on the OpenID Connect Provider
        get = new HttpGet(baseURL + "oidcclient/login?op=Keyloack");
        response = httpclient.execute(get, context);

        // 3. get login form from Keycloack and submit with expected username and password
        String action = StringUtils.substringBefore(
                StringUtils.substringAfter(EntityUtils.toString(response.getEntity()),
                        "<form id=\"kc-form-login\" onsubmit=\"login.disabled = true; return true;\" action=\""),
                "\" method=\"post\">").replace("&amp;", "&");

        List<NameValuePair> toSubmit = new ArrayList<>();
        toSubmit.add(new BasicNameValuePair("username", "john.doe"));
        toSubmit.add(new BasicNameValuePair("password", "password"));

        HttpPost post = new HttpPost(action);
        post.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        post.setEntity(new UrlEncodedFormEntity(toSubmit, Consts.UTF_8));
        response = httpclient.execute(post, context);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());

        // 4. verify that user is now authenticated
        get = new HttpGet(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).contains("john.doe"));
    }

    @Test
    public void sso2Console() throws IOException {
        sso("http://localhost:9080/syncope-console/");
    }

    @Test
    public void sso2Enduser() throws IOException {
        sso("http://localhost:9080/syncope-enduser/");
    }
}
