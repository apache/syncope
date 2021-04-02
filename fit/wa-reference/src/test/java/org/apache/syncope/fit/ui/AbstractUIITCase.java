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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public abstract class AbstractUIITCase extends AbstractITCase {

    protected static AuthPolicyTO getAuthPolicy() {
        String syncopeAuthModule = "DefaultSyncopeAuthModule";
        String ldapAuthModule = "DefaultLDAPAuthModule";
        String description = "UI auth policy";

        return policyService.list(PolicyType.AUTH).stream().
                map(AuthPolicyTO.class::cast).
                filter(policy -> description.equals(policy.getName())
                && policy.getConf() instanceof DefaultAuthPolicyConf
                && ((DefaultAuthPolicyConf) policy.getConf()).getAuthModules().contains(syncopeAuthModule)
                && ((DefaultAuthPolicyConf) policy.getConf()).getAuthModules().contains(ldapAuthModule)).
                findFirst().
                orElseGet(() -> {
                    DefaultAuthPolicyConf policyConf = new DefaultAuthPolicyConf();
                    policyConf.getAuthModules().add(syncopeAuthModule);
                    policyConf.getAuthModules().add(ldapAuthModule);

                    AuthPolicyTO policy = new AuthPolicyTO();
                    policy.setName(description);
                    policy.setConf(policyConf);

                    Response response = policyService.create(PolicyType.AUTH, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create Test Auth Policy");
                    }

                    return policyService.read(PolicyType.AUTH, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });
    }

    protected abstract void sso(String baseURL, String username, String password) throws IOException;

    @Test
    public void sso2Console() throws IOException {
        sso(CONSOLE_ADDRESS, "bellini", "password");
    }

    @Test
    public void sso2Enduser() throws IOException {
        sso(ENDUSER_ADDRESS, "bellini", "password");
    }

    @Test
    public void createUnmatching() throws IOException {
        try {
            userService.delete("pullFromLDAP");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        sso(CONSOLE_ADDRESS, "pullFromLDAP", "Password123");

        assertNotNull(userService.read("pullFromLDAP"));
    }

    protected abstract void doSelfReg(Runnable runnable);

    @Test
    public void selfRegUnmatching() throws IOException {
        try {
            userService.delete("pullFromLDAP");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        doSelfReg(() -> {
            try {
                sso(ENDUSER_ADDRESS, "pullFromLDAP", "Password123");
            } catch (IOException e) {
                fail(e);
            }
        });

        try {
            userService.read("pullFromLDAP");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }
}
