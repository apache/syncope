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

package org.apache.syncope.wa.starter.surrogate;

import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;

import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyncopeWASurrogateAuthenticationServiceTest extends AbstractTest {
    @Autowired
    private WARestClient wARestClient;

    @Autowired
    private SurrogateAuthenticationService surrogateService;

    @Test
    public void verifyImpersonation() {
        ImpersonationAccount account = new ImpersonationAccount.Builder().
            owner("syncope-principal").
            key("impersonatee").
            build();

        ImpersonationService impersonationService = wARestClient.
            getSyncopeClient().
            getService(ImpersonationService.class);

        Response response = impersonationService.create(account);
        assertNotNull(response);

        assertFalse(surrogateService.getEligibleAccountsForSurrogateToProxy(account.getOwner()).isEmpty());

        Principal principal = PrincipalFactoryUtils.newPrincipalFactory().createPrincipal(account.getOwner());
        assertFalse(surrogateService.canAuthenticateAs("unknown", principal, Optional.empty()));
        assertTrue(surrogateService.canAuthenticateAs(account.getKey(), principal, Optional.empty()));

    }
}
