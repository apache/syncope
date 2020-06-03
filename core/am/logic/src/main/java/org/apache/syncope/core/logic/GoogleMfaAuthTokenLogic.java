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
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Component
public class GoogleMfaAuthTokenLogic extends AbstractTransactionalLogic<AuthProfileTO> {
    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AuthProfileDataBinder authProfileDataBinder;

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Date expirationDate) {
        authProfileDAO.
            findAll().
            forEach(profile -> removeTokenAndSave(profile,
                token -> token.getIssueDate().compareTo(expirationDate) >= 0));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final Integer otp) {
        authProfileDAO.findByOwner(owner).
            ifPresent(profile -> removeTokenAndSave(profile,
                token -> token.getToken().equals(otp)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.setGoogleMfaAuthTokens(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Integer otp) {
        authProfileDAO.findAll().
            forEach(profile -> removeTokenAndSave(profile,
                token -> token.getToken().equals(otp)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll().
            forEach(profile -> {
                profile.setGoogleMfaAuthTokens(List.of());
                authProfileDAO.save(profile);
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_SAVE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthToken save(final GoogleMfaAuthToken token) {
        AuthProfile profile = authProfileDAO.findByOwner(token.getOwner()).
            orElseGet(() -> {
                final AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
                authProfile.setOwner(token.getOwner());
                return authProfile;
            });

        if (token.getKey() == null) {
            token.setKey(UUID.randomUUID().toString());
        }
        profile.add(token);
        profile = authProfileDAO.save(profile);
        return profile.getGoogleMfaAuthTokens().
            stream().
            filter(t -> t.getToken().equals(token.getToken())).
            findFirst().
            orElse(null);

    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthToken read(final String owner, final Integer otp) {
        return authProfileDAO.findByOwner(owner).
            stream().
            map(AuthProfile::getGoogleMfaAuthTokens).
            flatMap(List::stream).
            filter(token -> token.getToken().equals(otp)).
            findFirst().
            orElseThrow(() -> new NotFoundException("Could not find token for Owner " + owner + " and otp " + otp));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthToken read(final String key) {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getGoogleMfaAuthTokens).
            flatMap(List::stream).
            filter(token -> token.getKey().equals(key)).
            findFirst().
            orElse(null);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long countTokensFor(final String owner) {
        return authProfileDAO.findByOwner(owner).
            stream().
            mapToLong(profile -> profile.getGoogleMfaAuthTokens().size()).
            sum();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long countAll() {
        return authProfileDAO.findAll().
            stream().
            mapToLong(profile -> profile.getGoogleMfaAuthTokens().size()).
            sum();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthToken> findTokensFor(final String owner) {
        return authProfileDAO.findByOwner(owner).
            map(profile -> new ArrayList<>(profile.getGoogleMfaAuthTokens())).
            orElse(new ArrayList<>(0));
    }

    private void removeTokenAndSave(final AuthProfile profile, final Predicate<GoogleMfaAuthToken> criteria) {
        List<GoogleMfaAuthToken> tokens = profile.getGoogleMfaAuthTokens();
        boolean removed = tokens.removeIf(criteria);
        if (removed) {
            profile.setGoogleMfaAuthTokens(tokens);
            authProfileDAO.save(profile);
        }
    }

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
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
