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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.syncope.common.lib.OIDCStandardScope;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OIDCSRAITCase extends AbstractOIDCITCase {

    protected static final String CLIENT_CREDENTIALS_CLIENT_ID = "Client1";

    protected static final String CLIENT_CREDENTIALS_CLIENT_SECRET = "Client1";

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("oidc");
    }

    @BeforeAll
    public static void oidcTestsSetup() {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        // 1. client_credentials: add custom scope
        OIDCOpEntityTO entityTO = new OIDCOpEntityTO();
        entityTO.getCustomScopes().put(CUSTOM_SCOPE1, Set.of(CUSTOM_CLAIM1, CUSTOM_CLAIM2));
        OIDC_OP_ENTITY_SERVICE.set(entityTO);

        // 2. client_credentials: define attribute repository
        StubAttrRepoConf stubAttrRepoConf = new StubAttrRepoConf();
        stubAttrRepoConf.getAttributes().put(CUSTOM_CLAIM1, "value1");
        stubAttrRepoConf.getAttributes().put(CUSTOM_CLAIM2, "value2");
        AttrRepoTO attrRepoTO = new AttrRepoTO();
        attrRepoTO.setKey("Static");
        attrRepoTO.setConf(stubAttrRepoConf);
        ATTR_REPO_SERVICE.create(attrRepoTO);

        // 3. client_credentials: add attribute release policy
        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getReleaseAttrs().put(CUSTOM_CLAIM1, CUSTOM_CLAIM1);
        conf.getPrincipalAttrRepoConf().getAttrRepos().add("Static");
        AttrReleasePolicyTO policy = new AttrReleasePolicyTO();
        policy.setName("Filtered");
        policy.setConf(conf);
        Response response = POLICY_SERVICE.create(PolicyType.ATTR_RELEASE, policy);
        String policyKey = response.getHeaderString(RESTHeaders.RESOURCE_KEY);

        // 4. client_credentials: add client application
        OIDCRPClientAppTO clientCredentialsApp = new OIDCRPClientAppTO();
        clientCredentialsApp.setRealm(SyncopeConstants.ROOT_REALM);
        clientCredentialsApp.setClientAppId(999L);
        clientCredentialsApp.setName("Client1");
        clientCredentialsApp.setClientId(CLIENT_CREDENTIALS_CLIENT_ID);
        clientCredentialsApp.setClientSecret(CLIENT_CREDENTIALS_CLIENT_SECRET);
        clientCredentialsApp.setAttrReleasePolicy(policyKey);
        clientCredentialsApp.getRedirectUris().add("https://www.apache.org");
        clientCredentialsApp.setJwtAccessToken(true);
        clientCredentialsApp.setIdTokenSigningAlg(OIDCTokenSigningAlg.RS256);
        clientCredentialsApp.getScopes().add(OIDCStandardScope.openid.name());
        clientCredentialsApp.getScopes().add(CUSTOM_SCOPE1);
        clientCredentialsApp.setTokenEndpointAuthenticationMethod(OIDCClientAuthenticationMethod.client_secret_post);
        CLIENT_APP_SERVICE.create(ClientAppType.OIDCRP, clientCredentialsApp);

        // 5. prepare for SRA test cases
        Properties props = new Properties();
        try (InputStream propStream = OIDCSRAITCase.class.getResourceAsStream("/sra-oidc.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            fail("Could not load /sra-oidc.properties", e);
        }
        SRA_REGISTRATION_ID = "OIDC";
        CLIENT_APP_ID = 1L;
        SRA_CLIENT_ID = props.getProperty("sra.oidc.client-id");
        assertNotNull(SRA_CLIENT_ID);
        SRA_CLIENT_SECRET = props.getProperty("sra.oidc.client-secret");
        assertNotNull(SRA_CLIENT_SECRET);
        TOKEN_URI = WA_ADDRESS + "/oidc/accessToken";

        oidcClientAppSetup(
                OIDCSRAITCase.class.getName(), SRA_REGISTRATION_ID, CLIENT_APP_ID, SRA_CLIENT_ID, SRA_CLIENT_SECRET);
    }

    @Override
    protected boolean checkIdToken() {
        return true;
    }

    @Test
    public void clientCredentials() throws JsonProcessingException, ParseException {
        WebClient webclient = WebClient.create(WA_ADDRESS + "/oidc/oidcAccessToken");
        Form form = new Form().
                param(OAuth20Constants.GRANT_TYPE, OIDCGrantType.client_credentials.getExternalForm()).
                param(OAuth20Constants.CLIENT_ID, CLIENT_CREDENTIALS_CLIENT_ID).
                param(OAuth20Constants.CLIENT_SECRET, CLIENT_CREDENTIALS_CLIENT_SECRET).
                param(OAuth20Constants.SCOPE, "openid customScope1");
        Response response = webclient.form(form);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        JsonNode json = MAPPER.readTree(response.readEntity(String.class));

        assertTrue(json.has("access_token"));
        SignedJWT accessToken = SignedJWT.parse(json.get("access_token").asText());
        assertNotNull(accessToken.getJWTClaimsSet().getClaim(CUSTOM_CLAIM1));
        assertNull(accessToken.getJWTClaimsSet().getClaim(CUSTOM_CLAIM2));

        assertTrue(json.has("id_token"));
        SignedJWT idToken = SignedJWT.parse(json.get("id_token").asText());
        assertNotNull(idToken.getJWTClaimsSet().getClaim(CUSTOM_CLAIM1));
        assertNull(idToken.getJWTClaimsSet().getClaim(CUSTOM_CLAIM2));
    }
}
