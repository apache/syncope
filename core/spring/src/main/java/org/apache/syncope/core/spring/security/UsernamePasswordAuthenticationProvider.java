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
package org.apache.syncope.core.spring.security;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Configurable
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(UsernamePasswordAuthenticationProvider.class);

    protected final DomainOps domainOps;

    protected final AuthDataAccessor dataAccessor;

    protected final UserProvisioningManager provisioningManager;

    protected final DefaultCredentialChecker credentialChecker;

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    public UsernamePasswordAuthenticationProvider(
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final DefaultCredentialChecker credentialChecker,
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager) {

        this.domainOps = domainOps;
        this.dataAccessor = dataAccessor;
        this.provisioningManager = provisioningManager;
        this.credentialChecker = credentialChecker;
        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        String domainKey;
        Optional<Domain> domain;
        if (SyncopeConstants.MASTER_DOMAIN.equals(
                SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain())) {

            domainKey = SyncopeConstants.MASTER_DOMAIN;
            domain = Optional.empty();
        } else {
            domainKey = SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain();
            try {
                domain = Optional.of(domainOps.read(domainKey));
            } catch (NotFoundException | KeymasterException e) {
                throw new BadCredentialsException("Could not find domain " + domainKey, e);
            }
        }

        Mutable<String> username = new MutableObject<>();
        Boolean authenticated;
        Mutable<String> delegationKey = new MutableObject<>();

        if (securityProperties.getAnonymousUser().equals(authentication.getName())) {
            username.setValue(securityProperties.getAnonymousUser());
            credentialChecker.checkIsDefaultAnonymousKeyInUse();
            authenticated = authentication.getCredentials().toString().equals(securityProperties.getAnonymousKey());
        } else if (securityProperties.getAdminUser().equals(authentication.getName())) {
            username.setValue(securityProperties.getAdminUser());
            if (SyncopeConstants.MASTER_DOMAIN.equals(domainKey)) {
                credentialChecker.checkIsDefaultAdminPasswordInUse();
                authenticated = encryptorManager.getInstance().verify(
                        authentication.getCredentials().toString(),
                        securityProperties.getAdminPasswordAlgorithm(),
                        securityProperties.getAdminPassword());
            } else if (domain.isPresent()) {
                authenticated = encryptorManager.getInstance().verify(
                        authentication.getCredentials().toString(),
                        domain.get().getAdminCipherAlgorithm(),
                        domain.get().getAdminPassword());
            } else {
                LOG.error("Could not read admin credentials for domain {}", domainKey);
                authenticated = false;
            }
        } else {
            Triple<User, Boolean, String> authResult = AuthContextUtils.callAsAdmin(
                    domainKey,
                    () -> dataAccessor.authenticate(domainKey, authentication));
            authenticated = authResult.getMiddle();
            if (authResult.getLeft() != null && authResult.getMiddle() != null) {
                username.setValue(authResult.getLeft().getUsername());

                if (!authenticated) {
                    AuthContextUtils.runAsAdmin(domainKey, () -> provisioningManager.internalSuspend(
                            authResult.getLeft().getKey(), securityProperties.getAdminUser(), "Failed authentication"));
                }
            }
            delegationKey.setValue(authResult.getRight());
        }
        if (username.getValue() == null) {
            username.setValue(authentication.getPrincipal().toString());
        }

        return finalizeAuthentication(
                authenticated, domainKey, username.getValue(), delegationKey.getValue(), authentication);
    }

    protected Authentication finalizeAuthentication(
            final Boolean authenticated,
            final String domain,
            final String username,
            final String delegationKey,
            final Authentication authentication) {

        UsernamePasswordAuthenticationToken token;
        if (BooleanUtils.isTrue(authenticated)) {
            token = AuthContextUtils.callAsAdmin(domain, () -> {
                UsernamePasswordAuthenticationToken upat = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        dataAccessor.getAuthorities(username, delegationKey));
                upat.setDetails(authentication.getDetails());
                dataAccessor.audit(
                        domain,
                        username,
                        delegationKey,
                        OpEvent.Outcome.SUCCESS,
                        true,
                        authentication,
                        "Successfully authenticated, with entitlements: " + upat.getAuthorities());
                return upat;
            });

            LOG.debug("User {} successfully authenticated, with entitlements {}", username, token.getAuthorities());
        } else {
            dataAccessor.audit(
                    domain,
                    username,
                    delegationKey,
                    OpEvent.Outcome.FAILURE,
                    false,
                    authentication,
                    "Not authenticated");

            LOG.debug("User {} not authenticated", username);

            throw new BadCredentialsException("User " + username + " not authenticated");
        }

        return token;
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
