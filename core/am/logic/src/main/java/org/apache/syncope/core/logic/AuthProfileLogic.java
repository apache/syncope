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
package org.apache.syncope.core.logic;

import java.util.List;
import java.util.function.Predicate;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class AuthProfileLogic extends AbstractAuthProfileLogic {

    protected static <T> boolean updateIsNotSubsetOfCurrent(final List<T> current, final List<T> update) {
        return update.stream().anyMatch(Predicate.not(current::contains));
    }

    public AuthProfileLogic(
            final AuthProfileDataBinder binder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        super(binder, authProfileDAO, entityFactory);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_LIST + "')")
    @Transactional(readOnly = true)
    public Page<AuthProfileTO> search(final String keyword, final Pageable pageable) {
        long count = authProfileDAO.countByOwnerLike(keyword);
        List<AuthProfileTO> result = authProfileDAO.findByOwnerLike(keyword, pageable).
                stream().map(binder::getAuthProfileTO).toList();
        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    @Transactional(readOnly = true)
    public AuthProfileTO read(final String key) {
        return authProfileDAO.findById(key).
                map(binder::getAuthProfileTO).
                orElseThrow(() -> new NotFoundException("AuthProfile " + key));
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    @Transactional(readOnly = true)
    public AuthProfileTO selfRead() {
        return authProfileDAO.findByOwner(AuthContextUtils.getUsername()).
                map(binder::getAuthProfileTO).
                orElseThrow(() -> new NotFoundException("AuthProfile for " + AuthContextUtils.getUsername()));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_CREATE + "') ")
    public AuthProfileTO create(final AuthProfileTO authProfileTO) {
        return binder.getAuthProfileTO(authProfileDAO.save(binder.create(authProfileTO)));
    }

    protected void doUpdate(final AuthProfile authProfile, final AuthProfileTO authProfileTO) {
        binder.update(authProfile, authProfileTO);
        authProfileDAO.save(authProfile);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_UPDATE + "') ")
    public void update(final AuthProfileTO authProfileTO) {
        AuthProfile authProfile = authProfileDAO.findById(authProfileTO.getKey()).
                orElseThrow(() -> new NotFoundException("AuthProfile " + authProfileTO.getKey()));
        doUpdate(authProfile, authProfileTO);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    public void selfUpdate(final AuthProfileTO authProfileTO) {
        AuthProfile authProfile = authProfileDAO.findByOwner(AuthContextUtils.getUsername()).
                filter(p -> p.getKey().equals(authProfileTO.getKey()) && p.getOwner().equals(authProfileTO.getOwner())).
                orElseThrow(() -> new DelegatedAdministrationException(
                        SyncopeConstants.ROOT_REALM, AnyTypeKind.USER.name(), authProfileTO.getOwner()));

        if (updateIsNotSubsetOfCurrent(
                authProfile.getImpersonationAccounts(), authProfileTO.getImpersonationAccounts())
                || updateIsNotSubsetOfCurrent(
                        authProfile.getGoogleMfaAuthTokens(), authProfileTO.getGoogleMfaAuthTokens())
                || updateIsNotSubsetOfCurrent(
                        authProfile.getGoogleMfaAuthAccounts(), authProfileTO.getGoogleMfaAuthAccounts())
                || updateIsNotSubsetOfCurrent(
                        authProfile.getMfaTrustedDevices(), authProfileTO.getMfaTrustedDevices())
                || updateIsNotSubsetOfCurrent(
                        authProfile.getWebAuthnDeviceCredentials(), authProfileTO.getWebAuthnDeviceCredentials())
                || updateIsNotSubsetOfCurrent(
                        authProfile.getConsentDecisions(), authProfileTO.getConsentDecisions())) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Self update can only remove existing profile items");
            throw sce;
        }

        doUpdate(authProfile, authProfileTO);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void delete(final String key) {
        authProfileDAO.deleteById(key);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    public void selfDelete() {
        delete(authProfileDAO.findByOwner(AuthContextUtils.getUsername()).
                orElseThrow(() -> new NotFoundException("AuthProfile for " + AuthContextUtils.getUsername())).getKey());
    }
}
