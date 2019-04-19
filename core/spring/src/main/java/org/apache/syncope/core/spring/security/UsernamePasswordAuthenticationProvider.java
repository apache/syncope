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

import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Configurable
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(UsernamePasswordAuthenticationProvider.class);

    protected static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Autowired
    protected AuthDataAccessor dataAccessor;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    private DefaultCredentialChecker credentialChecker;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "adminPassword")
    protected String adminPassword;

    @Resource(name = "adminPasswordAlgorithm")
    protected String adminPasswordAlgorithm;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Resource(name = "anonymousKey")
    protected String anonymousKey;

    /**
     * @param adminPassword the adminPassword to set
     */
    public void setAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
    }

    /**
     * @param adminPasswordAlgorithm the adminPasswordAlgorithm to set
     */
    public void setAdminPasswordAlgorithm(final String adminPasswordAlgorithm) {
        this.adminPasswordAlgorithm = adminPasswordAlgorithm;
    }

    /**
     * @param anonymousKey the anonymousKey to set
     */
    public void setAnonymousKey(final String anonymousKey) {
        this.anonymousKey = anonymousKey;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        String domainKey = SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain();

        final String[] username = new String[1];
        Boolean authenticated;

        if (anonymousUser.equals(authentication.getName())) {
            username[0] = anonymousUser;
            credentialChecker.checkIsDefaultAnonymousKeyInUse();
            authenticated = authentication.getCredentials().toString().equals(anonymousKey);
        } else if (adminUser.equals(authentication.getName())) {
            username[0] = adminUser;
            if (SyncopeConstants.MASTER_DOMAIN.equals(domainKey)) {
                credentialChecker.checkIsDefaultAdminPasswordInUse();
                authenticated = ENCRYPTOR.verify(
                        authentication.getCredentials().toString(),
                        CipherAlgorithm.valueOf(adminPasswordAlgorithm),
                        adminPassword);
            } else {
                final String domainToFind = domainKey;
                authenticated = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
                    Domain domain = dataAccessor.findDomain(domainToFind);

                    return ENCRYPTOR.verify(
                            authentication.getCredentials().toString(),
                            domain.getAdminCipherAlgorithm(),
                            domain.getAdminPwd());
                });
            }
        } else {
            Pair<User, Boolean> authResult = AuthContextUtils.callAsAdmin(domainKey,
                    () -> dataAccessor.authenticate(domainKey, authentication));
            authenticated = authResult.getValue();
            if (authResult.getLeft() != null && authResult.getRight() != null) {
                username[0] = authResult.getLeft().getUsername();

                if (!authResult.getRight()) {
                    AuthContextUtils.callAsAdmin(domainKey, () -> {
                        provisioningManager.internalSuspend(authResult.getLeft().getKey());
                        return null;
                    });
                }
            }
        }
        if (username[0] == null) {
            username[0] = authentication.getPrincipal().toString();
        }

        final boolean isAuthenticated = authenticated != null && authenticated;
        UsernamePasswordAuthenticationToken token;
        if (isAuthenticated) {
            token = AuthContextUtils.callAsAdmin(domainKey, () -> {
                UsernamePasswordAuthenticationToken token1 = new UsernamePasswordAuthenticationToken(
                        username[0],
                        null,
                        dataAccessor.getAuthorities(username[0]));
                token1.setDetails(authentication.getDetails());
                dataAccessor.audit(
                        username[0],
                        AuditElements.EventCategoryType.LOGIC,
                        AuditElements.AUTHENTICATION_CATEGORY, null,
                        AuditElements.LOGIN_EVENT, Result.SUCCESS, null, isAuthenticated, authentication,
                        "Successfully authenticated, with entitlements: " + token1.getAuthorities());
                return token1;
            });

            LOG.debug("User {} successfully authenticated, with entitlements {}",
                    username[0], token.getAuthorities());
        } else {
            AuthContextUtils.callAsAdmin(domainKey, () -> {
                dataAccessor.audit(
                        username[0],
                        AuditElements.EventCategoryType.LOGIC,
                        AuditElements.AUTHENTICATION_CATEGORY,
                        null,
                        AuditElements.LOGIN_EVENT,
                        Result.FAILURE,
                        null,
                        isAuthenticated,
                        authentication,
                        "User " + username[0] + " not authenticated");
                return null;
            });

            LOG.debug("User {} not authenticated", username[0]);

            throw new BadCredentialsException("User " + username[0] + " not authenticated");
        }

        return token;
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
