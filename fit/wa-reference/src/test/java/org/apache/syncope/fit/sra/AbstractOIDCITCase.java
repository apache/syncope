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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.cxf.jaxrs.client.WebClient;
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
import org.apache.syncope.common.lib.OIDCScopeConstants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apereo.cas.oidc.OidcConstants;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

abstract class AbstractOIDCITCase extends AbstractSRAITCase {

    protected static String SRA_REGISTRATION_ID;

    protected static Long CLIENT_APP_ID;

    protected static String CLIENT_ID;

    protected static String CLIENT_SECRET;

    protected static String TOKEN_URI;

    protected static AttrReleasePolicyTO getAttrReleasePolicy() {
        String description = "SRA attr release policy";

        return POLICY_SERVICE.list(PolicyType.ATTR_RELEASE).stream().
                map(AttrReleasePolicyTO.class::cast).
                filter(policy -> description.equals(policy.getName())
                && policy.getConf() instanceof DefaultAttrReleasePolicyConf).
                findFirst().
                orElseGet(() -> {
                    DefaultAttrReleasePolicyConf policyConf = new DefaultAttrReleasePolicyConf();
                    policyConf.getReleaseAttrs().put("surname", "family_name");
                    policyConf.getReleaseAttrs().put("fullname", "name");
                    policyConf.getReleaseAttrs().put("firstname", "given_name");
                    policyConf.getReleaseAttrs().put("email", "email");
                    policyConf.getReleaseAttrs().put("groups", "groups");

                    AttrReleasePolicyTO policy = new AttrReleasePolicyTO();
                    policy.setName(description);
                    policy.setConf(policyConf);

                    Response response = POLICY_SERVICE.create(PolicyType.ATTR_RELEASE, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail(() -> "Could not create " + description);
                    }

                    return POLICY_SERVICE.read(
                            PolicyType.ATTR_RELEASE,
                            response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });
    }

    protected static void oidcClientAppSetup(
            final String appName,
            final String sraRegistrationId,
            final Long clientAppId,
            final String clientId,
            final String clientSecret) {

        OIDCRPClientAppTO clientApp = CLIENT_APP_SERVICE.list(ClientAppType.OIDCRP).stream().
                filter(app -> appName.equals(app.getName())).
                map(OIDCRPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    OIDCRPClientAppTO app = new OIDCRPClientAppTO();
                    app.setName(appName);
                    app.setRealm(SyncopeConstants.ROOT_REALM);
                    app.setClientAppId(clientAppId);
                    app.setClientId(clientId);
                    app.setClientSecret(clientSecret);
                    app.setBypassApprovalPrompt(false);

                    Response response = CLIENT_APP_SERVICE.create(ClientAppType.OIDCRP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create OIDC Client App");
                    }

                    return CLIENT_APP_SERVICE.read(
                            ClientAppType.OIDCRP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setJwtAccessToken(true);
        clientApp.setClientId(clientId);
        clientApp.setClientSecret(clientSecret);
        clientApp.setSubjectType(OIDCSubjectType.PUBLIC);
        clientApp.getRedirectUris().clear();
        clientApp.getRedirectUris().add(SRA_ADDRESS + "/login/oauth2/code/" + sraRegistrationId);
        clientApp.setSignIdToken(true);
        clientApp.setLogoutUri(SRA_ADDRESS + "/logout");
        clientApp.setAuthPolicy(getAuthPolicy().getKey());
        clientApp.setAttrReleasePolicy(getAttrReleasePolicy().getKey());
        clientApp.getScopes().add(OIDCScopeConstants.OPEN_ID);
        clientApp.getScopes().add(OIDCScopeConstants.PROFILE);
        clientApp.getScopes().add(OIDCScopeConstants.EMAIL);
        clientApp.getSupportedGrantTypes().add(OIDCGrantType.password);
        clientApp.getSupportedGrantTypes().add(OIDCGrantType.authorization_code);

        CLIENT_APP_SERVICE.update(ClientAppType.OIDCRP, clientApp);

        await().atMost(60, TimeUnit.SECONDS).pollInterval(20, TimeUnit.SECONDS).until(() -> {
            try {
                String metadata = WebClient.create(
                        WA_ADDRESS + "/oidc/" + OidcConstants.WELL_KNOWN_OPENID_CONFIGURATION_URL).
                        get().readEntity(String.class);
                if (!metadata.contains("groups")) {
                    WA_CONFIG_SERVICE.pushToWA(WAConfigService.PushSubject.conf, List.of());
                    throw new IllegalStateException();
                }
                metadata = WebClient.create(
                        WA_ADDRESS + "/actuator/env", ANONYMOUS_USER, ANONYMOUS_KEY, null).
                        get().readEntity(String.class);
                if (!metadata.contains("cas.authn.oidc.core.user-defined-scopes.syncope")) {
                    WA_CONFIG_SERVICE.pushToWA(WAConfigService.PushSubject.conf, List.of());
                    throw new IllegalStateException();
                }

                return true;
            } catch (Exception e) {
                // ignore
            }
            return false;
        });
        WA_CONFIG_SERVICE.pushToWA(WAConfigService.PushSubject.clientApps, List.of());
    }

    @Test
    void web() throws IOException {
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

        // 2a. redirected to WA login screen
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

        headers = checkGetResponse(response, originalRequestURI.replace("/protected", ""));
        assertTrue(headers.get(HttpHeaders.COOKIE).asText().contains("SESSION"));

        // 3. logout
        get = new HttpGet(SRA_ADDRESS + "/protected/logout");
        get.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        response = httpclient.execute(get, context);
        checkLogout(response);
    }

    @SuppressWarnings("unchecked")
    private void checkJWT(final String token, final boolean idToken) throws ParseException {
        assertNotNull(token);
        SignedJWT jwt = SignedJWT.parse(token);
        assertNotNull(jwt);
        JWTClaimsSet idTokenClaimsSet = jwt.getJWTClaimsSet();
        assertEquals("verdi", idTokenClaimsSet.getSubject());
        if (idToken) {
            assertEquals("verdi", idTokenClaimsSet.getStringClaim("preferred_username"));
        }
        assertEquals("verdi@syncope.org", idTokenClaimsSet.getStringClaim("email"));
        assertEquals("Verdi", idTokenClaimsSet.getStringClaim("family_name"));
        assertEquals("Giuseppe", idTokenClaimsSet.getStringClaim("given_name"));
        assertEquals("Giuseppe Verdi", idTokenClaimsSet.getStringClaim("name"));
        List<Object> groups = idTokenClaimsSet.getListClaim("groups");
        assertEquals(3, groups.size());
        groups.stream().anyMatch(g -> ((Map<String, String>) g).equals(Map.of("groupName", "root")));
        groups.stream().anyMatch(g -> ((Map<String, String>) g).equals(Map.of("groupName", "child")));
        groups.stream().anyMatch(g -> ((Map<String, String>) g).equals(Map.of("groupName", "citizen")));
    }

    protected abstract boolean checkIdToken();

    @Test
    void rest() throws IOException, ParseException {
        // 0. access public route
        WebClient client = WebClient.create(SRA_ADDRESS + "/public/post").
                accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON);
        Response response = client.post(null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        // 1. obtain id and access tokens
        Form form = new Form().
                param("grant_type", "password").
                param("client_id", CLIENT_ID).
                param("client_secret", CLIENT_SECRET).
                param("username", "verdi").
                param("password", "password").
                param("scope", "openid profile email syncope");
        response = WebClient.create(TOKEN_URI).post(form);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_TYPE).startsWith(MediaType.APPLICATION_JSON));

        JsonNode json = MAPPER.readTree(response.readEntity(String.class));

        if (checkIdToken()) {
            // 1a. take and verify id_token
            String idToken = json.get("id_token").asText();
            assertNotNull(idToken);
            checkJWT(idToken, true);
        }

        // 1b. take and verify access_token
        String accessToken = json.get("access_token").asText();
        checkJWT(accessToken, false);

        // 2. access protected route
        client = WebClient.create(SRA_ADDRESS + "/protected/post").
                authorization("Bearer " + accessToken).
                accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON);
        response = client.post(null);

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        json = MAPPER.readTree(response.readEntity(String.class));

        ObjectNode headers = (ObjectNode) json.get("headers");
        assertEquals(MediaType.APPLICATION_JSON, headers.get(HttpHeaders.ACCEPT).asText());
        assertEquals(MediaType.APPLICATION_JSON, headers.get(HttpHeaders.CONTENT_TYPE).asText());
        assertThat(headers.get("X-Forwarded-Host").asText(), is(oneOf("localhost:8080", "127.0.0.1:8080")));

        String withHost = client.getBaseURI().toASCIIString().replace("/protected", "");
        String withIP = withHost.replace("localhost", "127.0.0.1");
        assertThat(json.get("url").asText(), is(oneOf(withHost, withIP)));
    }
}
