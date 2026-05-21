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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MultitenancyITCase extends AbstractITCase {

    protected static final String MT_USERNAME = "multitenancy";

    protected static final String MT_PASSWORD = "password";

    @BeforeAll
    public static void multitenancyCheck() {
        CLIENT_FACTORY = new SyncopeClientFactoryBean().setAddress(CORE_ADDRESS).setDomain("Two");

        ADMIN_CLIENT = CLIENT_FACTORY.create(ADMIN_UNAME, "password2");

        // 1. create Syncope auth module
        AuthModuleService authModuleService = ADMIN_CLIENT.getService(AuthModuleService.class);
        AuthModuleTO syncopeAuthModule = null;
        try {
            syncopeAuthModule = authModuleService.read("syncopeTwo");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                SyncopeAuthModuleConf conf = new SyncopeAuthModuleConf();
                conf.setDomain("Two");

                syncopeAuthModule = new AuthModuleTO();
                syncopeAuthModule.setKey("syncopeTwo");
                syncopeAuthModule.setConf(conf);

                Response response = authModuleService.create(syncopeAuthModule);
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            }
        }
        assertNotNull(syncopeAuthModule);

        // 2. create user
        assertNull(ADMIN_CLIENT.getService(RealmService.class).
                search(new RealmQuery.Builder().build()).getResult().getFirst().getPasswordPolicy());

        UserService userService = ADMIN_CLIENT.getService(UserService.class);
        UserTO user = null;
        try {
            user = userService.read(MT_USERNAME);
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                UserCR userCR = new UserCR();
                userCR.setRealm(SyncopeConstants.ROOT_REALM);
                userCR.setUsername(MT_USERNAME);
                userCR.setPassword(MT_PASSWORD);

                Response response = ADMIN_CLIENT.getService(UserService.class).create(userCR);
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

                ProvisioningResult<UserTO> result = response.readEntity(new GenericType<>() {
                });
                user = result.getEntity();
            }
        }
        assertNotNull(user);
    }

    @Test
    public void login() throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(new BasicCookieStore());

            String loginPageBody;
            try (CloseableHttpResponse response =
                    httpclient.execute(new HttpGet(WA_ADDRESS + "/tenants/Two/login"), context)) {

                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                loginPageBody = EntityUtils.toString(response.getEntity());
            }
            assertNotNull(loginPageBody);

            String location;
            try (CloseableHttpResponse response =
                    authenticateToWA(MT_USERNAME, MT_PASSWORD, loginPageBody, httpclient, context, "Two")) {

                assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
                location = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
            }
            assertTrue(location.endsWith("/account"));

            try (CloseableHttpResponse response =
                    httpclient.execute(new HttpGet(WA_ADDRESS + "/tenants/Two/account"), context)) {

                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(EntityUtils.toString(response.getEntity()).contains(MT_USERNAME));
            }
        }
    }
}
