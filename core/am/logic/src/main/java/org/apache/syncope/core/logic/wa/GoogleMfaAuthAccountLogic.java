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
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class GoogleMfaAuthAccountLogic extends AbstractAuthProfileLogic {

    protected final EntityFactory entityFactory;

    public GoogleMfaAuthAccountLogic(
            final EntityFactory entityFactory,
            final AuthProfileDAO authProfileDAO,
            final AuthProfileDataBinder binder) {

        super(authProfileDAO, binder);
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthAccount> list() {
        return authProfileDAO.findAll(-1, -1).
                stream().
                map(AuthProfile::getGoogleMfaAuthAccounts).
                filter(Objects::nonNull).
                flatMap(List::stream).
                collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.setGoogleMfaAuthAccounts(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final long id) {
        authProfileDAO.findAll(-1, -1).
                stream().
                filter(Objects::nonNull).
                filter(profile -> profile.
                getGoogleMfaAuthAccounts().
                stream().
                allMatch(acct -> acct.getId() == id)).
                findFirst().
                ifPresentOrElse(profile -> {
                    if (profile.getGoogleMfaAuthAccounts().removeIf(acct -> acct.getId() == id)) {
                        authProfileDAO.save(profile);
                    }
                },
                        () -> {
                            throw new NotFoundException("Could not find account for id " + id);
                        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll(-1, -1).forEach(profile -> {
            profile.setGoogleMfaAuthAccounts(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void create(final String owner, final GoogleMfaAuthAccount account) {
        AuthProfile profile = authProfileDAO.findByOwner(owner).orElseGet(() -> {
            AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
            authProfile.setOwner(owner);
            return authProfile;
        });

        List<GoogleMfaAuthAccount> accounts = profile.getGoogleMfaAuthAccounts();
        accounts.add(account);
        profile.setGoogleMfaAuthAccounts(accounts);
        authProfileDAO.save(profile);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void update(final String owner, final GoogleMfaAuthAccount account) {
        AuthProfile authProfile = authProfileDAO.findByOwner(owner).
                orElseThrow(() -> new NotFoundException("Could not find account for Owner " + owner));
        List<GoogleMfaAuthAccount> accounts = authProfile.getGoogleMfaAuthAccounts();
        if (accounts.removeIf(acct -> acct.getId() == account.getId())) {
            accounts.add(account);
            authProfile.setGoogleMfaAuthAccounts(accounts);
            authProfileDAO.save(authProfile);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public List<GoogleMfaAuthAccount> read(final String owner) {
        return authProfileDAO.findByOwner(owner).
                stream().
                map(AuthProfile::getGoogleMfaAuthAccounts).
                filter(Objects::nonNull).
                filter(accounts -> !accounts.isEmpty()).
                findFirst().
                orElseThrow(() -> new NotFoundException("Could not find account for Owner " + owner));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthAccount read(final long id) {
        return authProfileDAO.findAll(-1, -1).
                stream().
                map(AuthProfile::getGoogleMfaAuthAccounts).
                filter(Objects::nonNull).
                map(accounts -> accounts.stream().
                filter(acct -> acct.getId() == id).
                findFirst().
                orElse(null)).
                filter(Objects::nonNull).
                findFirst().
                orElse(null);
    }
}
