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
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.InvalidOneTimeTokenException;
import org.springframework.security.core.Authentication;

@Configurable
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(UsernamePasswordAuthenticationProvider.class);

    protected final DomainOps domainOps;

    protected final AuthDataAccessor dataAccessor;

    protected final UserProvisioningManager provisioningManager;

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    public UsernamePasswordAuthenticationProvider(
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager) {

        this.domainOps = domainOps;
        this.dataAccessor = dataAccessor;
        this.provisioningManager = provisioningManager;
        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        String domainKey = SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain();
        Optional<Domain> domain;
        if (SyncopeConstants.MASTER_DOMAIN.equals(domainKey)) {
            domain = Optional.empty();
        } else {
            try {
                domain = Optional.of(domainOps.read(domainKey));
            } catch (NotFoundException | KeymasterException e) {
                throw new BadCredentialsException("Could not find domain " + domainKey, e);
            }
        }

        AuthDataAccessor.UsernamePasswordAuthResult authResult = AuthContextUtils.callAsAdmin(
                domainKey,
                () -> dataAccessor.authenticate(domain, authentication));

        Mutable<String> username = new MutableObject<>();
        if (authResult.user() != null) {
            username.setValue(authResult.user().getUsername());

            if (!authResult.isSuccess()) {
                AuthContextUtils.runAsAdmin(domainKey, () -> provisioningManager.internalSuspend(
                        authResult.user().getKey(), securityProperties.getAdminUser(), "Failed authentication"));
            }
        }

        if (username.get() == null) {
            username.setValue(authentication.getPrincipal().toString());
        }

        return finalizeAuthentication(
                domainKey,
                username.get(),
                authResult,
                authentication);
    }

    protected Authentication finalizeAuthentication(
            final String domain,
            final String username,
            final AuthDataAccessor.UsernamePasswordAuthResult authResult,
            final Authentication authentication) {

        if (authResult.isSuccess()) {
            UsernamePasswordAuthenticationToken token = AuthContextUtils.callAsAdmin(domain, () -> {
                UsernamePasswordAuthenticationToken upat = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authResult.authorities());
                upat.setDetails(authentication.getDetails());
                dataAccessor.audit(
                        domain,
                        username,
                        authResult.delegationKey(),
                        OpEvent.Outcome.SUCCESS,
                        true,
                        authentication,
                        "Successfully authenticated, with entitlements: " + authResult.authorities());
                return upat;
            });

            LOG.debug("User {} successfully authenticated, with entitlements {}", username, authResult.authorities());
            return token;
        }

        dataAccessor.audit(
                domain,
                username,
                null,
                OpEvent.Outcome.FAILURE,
                false,
                authentication,
                "Not authenticated");

        LOG.debug("User {} not authenticated", username);

        if (!authResult.passwordVerified()) {
            throw new BadCredentialsException(username + ": invalid password provided");
        }
        throw new InvalidOneTimeTokenException(username + ": invalid OTP or recovery code provided");
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
