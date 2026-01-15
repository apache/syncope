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
package org.apache.syncope.core.logic.wa;

import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.WebAuthnAccount;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class WebAuthnRegistrationLogic extends AbstractAuthProfileLogic {

    public WebAuthnRegistrationLogic(
            final AuthProfileDataBinder binder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        super(binder, authProfileDAO, entityFactory);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WebAuthnAccount> list() {
        return authProfileDAO.findAll(Pageable.unpaged()).stream().
                map(profile -> new WebAuthnAccount.Builder().
                credentials(profile.getWebAuthnDeviceCredentials()).build()).
                toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WebAuthnAccount read(final String owner) {
        return authProfileDAO.findByOwner(owner).
                map(profile -> new WebAuthnAccount.Builder().
                credentials(profile.getWebAuthnDeviceCredentials()).build()).
                orElseThrow(() -> new NotFoundException("Could not find account for Owner " + owner));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.getWebAuthnDeviceCredentials().clear();
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final String credentialId) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            if (profile.getWebAuthnDeviceCredentials().removeIf(acct -> acct.getIdentifier().equals(credentialId))) {
                authProfileDAO.save(profile);
            }
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void create(final String owner, final WebAuthnAccount account) {
        AuthProfile profile = authProfile(owner);

        profile.getWebAuthnDeviceCredentials().clear();
        account.getCredentials().forEach(profile::add);

        authProfileDAO.save(profile);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void update(final String owner, final WebAuthnAccount account) {
        AuthProfile profile = authProfileDAO.findByOwner(owner).
                orElseThrow(() -> new NotFoundException("Could not find account for Owner " + owner));

        profile.getWebAuthnDeviceCredentials().clear();
        account.getCredentials().forEach(profile::add);

        authProfileDAO.save(profile);
    }
}
