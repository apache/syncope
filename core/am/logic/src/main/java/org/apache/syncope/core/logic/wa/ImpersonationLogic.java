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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImpersonationLogic extends AbstractAuthProfileLogic {

    @Autowired
    private EntityFactory entityFactory;

    @PreAuthorize("hasRole('" + AMEntitlement.IMPERSONATION_READ_ACCOUNT + "')"
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public List<ImpersonationAccount> findByOwner(final String owner) {
        return authProfileDAO.findByOwner(owner).map(AuthProfile::getImpersonationAccounts).orElse(List.of());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.IMPERSONATION_READ_ACCOUNT + "')"
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public ImpersonationAccount find(final String owner, final String id) {
        return authProfileDAO.findByOwner(owner)
            .map(AuthProfile::getImpersonationAccounts)
            .stream()
            .flatMap(List::stream)
            .filter(acct -> acct.getId().equalsIgnoreCase(id))
            .findFirst()
            .orElseThrow(() -> {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
                sce.getElements().add(owner + " is not authorized to impersonate " + id);
                throw sce;
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.IMPERSONATION_CREATE_ACCOUNT + "')"
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public String create(final ImpersonationAccount account) {
        AuthProfile profile = authProfileDAO.findByOwner(account.getOwner()).orElseGet(() -> {
            AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
            authProfile.setOwner(account.getOwner());
            return authProfile;
        });

        if (profile.getImpersonationAccounts()
            .stream()
            .noneMatch(acct -> acct.getId().equalsIgnoreCase(account.getId()))) {
            final List<ImpersonationAccount> accounts = new ArrayList<>(profile.getImpersonationAccounts());
            accounts.add(account);
            profile.setImpersonationAccounts(accounts);
        }
        return authProfileDAO.save(profile).getKey();
    }
}
