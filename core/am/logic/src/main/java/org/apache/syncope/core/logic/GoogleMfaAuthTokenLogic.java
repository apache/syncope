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
import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.provisioning.api.data.GoogleMfaAuthTokenDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoogleMfaAuthTokenLogic extends AbstractTransactionalLogic<GoogleMfaAuthTokenTO> {
    @Autowired
    private GoogleMfaAuthTokenDataBinder googleMfaAuthTokenDataBinder;

    @Autowired
    private AuthProfileDataBinder authProfileDataBinder;

    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Override
    protected GoogleMfaAuthTokenTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String owner = null;
        Integer token = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; owner == null && token == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    owner = (String) args[i];
                } else if (args[i] instanceof Integer) {
                    token = (Integer) args[i];
                } else if (args[i] instanceof GoogleMfaAuthTokenTO) {
                    owner = ((GoogleMfaAuthTokenTO) args[i]).getOwner();
                    token = ((GoogleMfaAuthTokenTO) args[i]).getToken();
                }
            }
        }

        if (owner != null && token != null) {
            try {
                return read(owner, token);
            } catch (final Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }
        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Date expirationDate) {
        authProfileDAO.findAll().
            forEach(profile -> {
                boolean removed = profile.getGoogleMfaAuthTokens().
                    removeIf(token -> token.getIssueDate().compareTo(expirationDate) >= 0);
                if (removed) {
                    authProfileDAO.save(profile);
                }
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final Integer otp) {
        authProfileDAO.findByOwner(owner).
            ifPresent(profile -> {
                boolean removed = profile.getGoogleMfaAuthTokens().
                    removeIf(token -> token.getToken().equals(otp));
                if (removed) {
                    authProfileDAO.save(profile);
                }
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.getGoogleMfaAuthTokens().clear();
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Integer otp) {
        authProfileDAO.findAll().
            forEach(profile -> {
                boolean removed = profile.getGoogleMfaAuthTokens().
                    removeIf(token -> token.getToken().equals(otp));
                if (removed) {
                    authProfileDAO.save(profile);
                }
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll().
            forEach(profile -> {
                profile.getGoogleMfaAuthTokens().clear();
                authProfileDAO.save(profile);
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_SAVE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthTokenTO save(final GoogleMfaAuthTokenTO tokenTO) {
        AuthProfile profile = authProfileDAO.findByOwner(tokenTO.getOwner()).
            orElse(authProfileDataBinder.create(tokenTO.getOwner()));
        profile.getGoogleMfaAuthTokens().add(googleMfaAuthTokenDataBinder.create(tokenTO));
        profile = authProfileDAO.save(profile);
        return profile.getGoogleMfaAuthTokens().
            stream().
            filter(token -> token.getToken().equals(tokenTO.getToken())).
            findFirst().
            map(token -> googleMfaAuthTokenDataBinder.getGoogleMfaAuthTokenTO(token)).
            orElse(null);

    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthTokenTO read(final String owner, final Integer otp) {
        return authProfileDAO.findByOwner(owner).
            stream().
            map(AuthProfile::getGoogleMfaAuthTokens).
            flatMap(List::stream).
            filter(token -> token.getToken().equals(otp)).
            findFirst().
            map(googleMfaAuthTokenDataBinder::getGoogleMfaAuthTokenTO).
            orElse(null);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthTokenTO read(final String key) {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getGoogleMfaAuthTokens).
            flatMap(List::stream).
            filter(token -> token.getKey().equals(key)).
            findFirst().
            map(googleMfaAuthTokenDataBinder::getGoogleMfaAuthTokenTO).
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
    public List<GoogleMfaAuthTokenTO> findTokensFor(final String owner) {
        return authProfileDAO.findByOwner(owner).
            map(profile -> profile.getGoogleMfaAuthTokens().
                stream().
                map(googleMfaAuthTokenDataBinder::getGoogleMfaAuthTokenTO).
                collect(Collectors.toList())).
            orElse(List.of());
    }
}
