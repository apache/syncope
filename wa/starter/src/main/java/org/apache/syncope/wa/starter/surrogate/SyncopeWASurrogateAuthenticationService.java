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
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;

import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class SyncopeWASurrogateAuthenticationService implements SurrogateAuthenticationService {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASurrogateAuthenticationService.class);

    private final WARestClient waRestClient;

    public SyncopeWASurrogateAuthenticationService(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public boolean canAuthenticateAs(final String surrogate, final Principal principal,
                                     final Optional<Service> service) {
        LOG.debug("Checking impersonation attempt by {} for {}", principal, surrogate);
        Response response = getImpersonationService().find(principal.getId(), surrogate,
            service.map(Service::getId).orElse(null));
        return response != null && HttpStatus.valueOf(response.getStatus()).is2xxSuccessful();
    }

    @Override
    public Collection<String> getEligibleAccountsForSurrogateToProxy(final String username) {
        return getImpersonationService().findByOwner(username).
            stream().
            map(ImpersonationAccount::getId).
            collect(Collectors.toList());
    }

    private ImpersonationService getImpersonationService() {
        if (!WARestClient.isReady()) {
            throw new RuntimeException("Syncope core is not yet ready");
        }
        return waRestClient.getSyncopeClient().getService(ImpersonationService.class);
    }
}
