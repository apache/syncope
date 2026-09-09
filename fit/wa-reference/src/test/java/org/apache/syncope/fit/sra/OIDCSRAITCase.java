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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OIDCSRAITCase extends AbstractOIDCITCase {

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("oidc");
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
        SRA_REGISTRATION_ID = "OIDC";
        CLIENT_APP_ID = 1L;
        CLIENT_ID = props.getProperty("sra.oidc.client-id");
        assertNotNull(CLIENT_ID);
        CLIENT_SECRET = props.getProperty("sra.oidc.client-secret");
        assertNotNull(CLIENT_SECRET);
        TOKEN_URI = WA_ADDRESS + "/oidc/accessToken";

        oidcClientAppSetup(
                OIDCSRAITCase.class.getName(), SRA_REGISTRATION_ID, CLIENT_APP_ID, CLIENT_ID, CLIENT_SECRET);
    }

    @SuppressWarnings("unchecked")
    private void checkJWT(final String token, final boolean idToken) throws Exception {
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

    @Test
    void rest() throws Exception {
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

        // 1a. take and verify id_token
        String idToken = json.get("id_token").asText();
        assertNotNull(idToken);
        checkJWT(idToken, true);

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
