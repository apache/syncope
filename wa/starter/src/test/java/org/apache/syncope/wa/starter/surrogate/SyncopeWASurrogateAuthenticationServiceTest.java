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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.AbstractTest;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeWASurrogateAuthenticationServiceTest extends AbstractTest {

    @Autowired
    private WARestClient waRestClient;

    @Autowired
    private SurrogateAuthenticationService surrogateService;

    @Test
    public void verifyImpersonation() {
        String owner = "syncope-principal";
        ImpersonationAccount account = new ImpersonationAccount.Builder().impersonated("impersonatee").
                build();

        ImpersonationService impersonationService = waRestClient.getSyncopeClient().
                getService(ImpersonationService.class);

        impersonationService.create(owner, account);

        assertFalse(surrogateService.getEligibleAccountsForSurrogateToProxy(owner).isEmpty());

        Principal principal = PrincipalFactoryUtils.newPrincipalFactory().createPrincipal(owner);
        assertFalse(surrogateService.canAuthenticateAs("unknown", principal, Optional.empty()));
        assertTrue(surrogateService.canAuthenticateAs(account.getImpersonated(), principal, Optional.empty()));
    }
}
