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

import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GoogleMfaAuthTokenLogic extends AbstractAuthProfileLogic {

    @Autowired
    private EntityFactory entityFactory;

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Date expirationDate) {
        authProfileDAO.findAll().forEach(profile -> removeTokenAndSave(
                profile, token -> token.getIssueDate().compareTo(expirationDate) >= 0));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final int otp) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> removeTokenAndSave(
                profile, token -> token.getOtp() == otp));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteFor(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.setGoogleMfaAuthTokens(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final int otp) {
        authProfileDAO.findAll().forEach(profile -> removeTokenAndSave(
                profile, token -> token.getOtp() == otp));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll().forEach(profile -> {
            profile.setGoogleMfaAuthTokens(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_STORE_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public String store(final String owner, final GoogleMfaAuthToken token) {
        if (token.getKey() == null) {
            token.setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        AuthProfile profile = authProfileDAO.findByOwner(owner).orElseGet(() -> {
            AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
            authProfile.setOwner(owner);
            return authProfile;
        });

        List<GoogleMfaAuthToken> tokens = profile.getGoogleMfaAuthTokens();
        tokens.add(token);
        profile.setGoogleMfaAuthTokens(tokens);
        authProfileDAO.save(profile);
        return token.getKey();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthToken readFor(final String owner, final int otp) {
        return authProfileDAO.findByOwner(owner).
                stream().
                map(AuthProfile::getGoogleMfaAuthTokens).
                flatMap(List::stream).
                filter(token -> token.getOtp() == otp).
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
                orElseThrow(() -> new NotFoundException("Could not find token for " + key));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_LIST_TOKENS + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthToken> list() {
        return authProfileDAO.findAll().stream().
                map(AuthProfile::getGoogleMfaAuthTokens).
                flatMap(List::stream).
                collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthToken> readFor(final String owner) {
        return authProfileDAO.findByOwner(owner).
                map(AuthProfile::getGoogleMfaAuthTokens).
                orElse(List.of());
    }

    private void removeTokenAndSave(final AuthProfile profile, final Predicate<GoogleMfaAuthToken> criteria) {
        List<GoogleMfaAuthToken> tokens = profile.getGoogleMfaAuthTokens();
        if (tokens.removeIf(criteria)) {
            profile.setGoogleMfaAuthTokens(tokens);
            authProfileDAO.save(profile);
        }
    }
}
