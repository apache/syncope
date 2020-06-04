/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.logic;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GoogleMfaAuthAccountLogic extends AbstractTransactionalLogic<AuthProfileTO> {
    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AuthProfileDataBinder authProfileDataBinder;

    @Override
    protected AuthProfileTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AuthProfileTO) {
                    key = ((AuthProfileTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return authProfileDAO.findByKey(key).
                    map(authProfileDataBinder::getAuthProfileTO).
                    orElseThrow();
            } catch (final Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }
        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAccountFor(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.setGoogleMfaAuthAccount(null);
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll().
            forEach(profile -> {
                profile.setGoogleMfaAuthAccount(null);
                authProfileDAO.save(profile);
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_SAVE_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthAccount save(final GoogleMfaAuthAccount acct) {
        AuthProfile profile = authProfileDAO.findByOwner(acct.getOwner()).
            orElseGet(() -> {
                final AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
                authProfile.setOwner(acct.getOwner());
                return authProfile;
            });

        if (acct.getKey() == null) {
            acct.setKey(UUID.randomUUID().toString());
        }
        profile.setGoogleMfaAuthAccount(acct);
        return authProfileDAO.save(profile).getGoogleMfaAuthAccount();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_UPDATE_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void update(final GoogleMfaAuthAccount acct) {
        AuthProfile profile = authProfileDAO.findByOwner(acct.getOwner()).
            orElseThrow(() -> new NotFoundException("Could not find account for Owner " + acct.getOwner()));
        profile.setGoogleMfaAuthAccount(acct);
        authProfileDAO.save(profile).getGoogleMfaAuthAccount();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthAccount findAccountFor(final String owner) {
        return authProfileDAO.findByOwner(owner).
            stream().
            map(AuthProfile::getGoogleMfaAuthAccount).
            filter(Objects::nonNull).
            findFirst().
            orElseThrow(() -> new NotFoundException("Could not find account for Owner " + owner));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthAccount findAccountBy(final String key) {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getGoogleMfaAuthAccount).
            filter(Objects::nonNull).
            filter(acct -> acct.getKey().equals(key)).
            findFirst().
            orElse(null);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_ACCOUNTS + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public long countAll() {
        return authProfileDAO.findAll().
            stream().
            filter(profile -> profile.getGoogleMfaAuthAccount() != null).
            count();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_ACCOUNT + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAccountBy(final String key) {
        authProfileDAO.findAll().
            stream().
            filter(profile -> profile.getGoogleMfaAuthAccount() != null
                && profile.getGoogleMfaAuthAccount().getKey().equals(key)).
            findFirst().
            ifPresent(profile -> {
                profile.setGoogleMfaAuthAccount(null);
                authProfileDAO.save(profile);
            });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthAccount> list() {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getGoogleMfaAuthAccount).
            filter(Objects::nonNull).
            collect(Collectors.toList());
    }
}
