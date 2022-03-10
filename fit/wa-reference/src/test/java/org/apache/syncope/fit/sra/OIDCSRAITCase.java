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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OIDCSRAITCase extends AbstractSRAITCase {

    protected static String CLIENT_ID;

    protected static String CLIENT_SECRET;

    protected static String TOKEN_URI;

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("oidc");
    }

    protected static void oidcClientAppSetup(
            final String appName,
            final String sraRegistrationId,
            final Long clientAppId,
            final String clientId,
            final String clientSecret) {

        OIDCRPClientAppTO clientApp = clientAppService.list(ClientAppType.OIDCRP).stream().
                filter(app -> appName.equals(app.getName())).
                map(OIDCRPClientAppTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    OIDCRPClientAppTO app = new OIDCRPClientAppTO();
                    app.setName(appName);
                    app.setClientAppId(clientAppId);
                    app.setClientId(clientId);
                    app.setClientSecret(clientSecret);

                    Response response = clientAppService.create(ClientAppType.OIDCRP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create OIDC Client App");
                    }

                    return clientAppService.read(
                            ClientAppType.OIDCRP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setJwtAccessToken(true);
        clientApp.setClientId(clientId);
        clientApp.setClientSecret(clientSecret);
        clientApp.setSubjectType(OIDCSubjectType.PUBLIC);
        clientApp.getRedirectUris().clear();
        clientApp.getRedirectUris().add(SRA_ADDRESS + "/login/oauth2/code/" + sraRegistrationId);
        clientApp.setAuthPolicy(getAuthPolicy().getKey());
        clientApp.setSignIdToken(true);
        clientApp.setLogoutUri(SRA_ADDRESS + "/logout");

        clientAppService.update(ClientAppType.OIDCRP, clientApp);
        clientAppService.pushToWA();
    }

    @BeforeAll
    public static void clientAppSetup() {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        Properties props = new Properties();
        try (InputStream propStream = OIDCSRAITCase.class.getResourceAsStream("/sra-oidc.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            fail("Could not load /sra-oidc.properties", e);
        }
        CLIENT_ID = props.getProperty("sra.oidc.client-id");
        assertNotNull(CLIENT_ID);
        CLIENT_SECRET = props.getProperty("sra.oidc.client-secret");
        assertNotNull(CLIENT_SECRET);
        TOKEN_URI = WA_ADDRESS + "/oidc/accessToken";

        oidcClientAppSetup(OIDCSRAITCase.class.getName(), "OIDC", 1L, CLIENT_ID, CLIENT_SECRET);
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

    protected void checkIdToken(final JsonNode json) throws ParseException {
        SignedJWT idToken = SignedJWT.parse(json.get("id_token").asText());
        assertNotNull(idToken);
        JWTClaimsSet idTokenClaimsSet = idToken.getJWTClaimsSet();
        assertEquals("verdi", idTokenClaimsSet.getStringClaim("preferred_username"));
        assertEquals("verdi@syncope.org", idTokenClaimsSet.getStringClaim("email"));
        assertEquals("Verdi", idTokenClaimsSet.getStringClaim("family_name"));
        assertEquals("Giuseppe", idTokenClaimsSet.getStringClaim("given_name"));
        assertEquals("Giuseppe Verdi", idTokenClaimsSet.getStringClaim("name"));
        assertEquals(Set.of("root", "child", "citizen"), Set.of(idTokenClaimsSet.getStringArrayClaim("groups")));
    }

    @Test
    public void rest() throws IOException, ParseException {
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
                param("scope", "openid profile email address phone offline_access syncope");
        response = WebClient.create(TOKEN_URI).post(form);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_TYPE).startsWith(MediaType.APPLICATION_JSON));

        JsonNode json = MAPPER.readTree(response.readEntity(String.class));

        // 1a. verify id_token
        checkIdToken(json);

        // 1b. take access_token
        String accessToken = json.get("access_token").asText();
        assertNotNull(accessToken);

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
        assertEquals("localhost:" + PORT, headers.get("X-Forwarded-Host").asText());

        assertEquals(client.getBaseURI().toASCIIString().replace("/protected", ""), json.get("url").asText());
    }
}
