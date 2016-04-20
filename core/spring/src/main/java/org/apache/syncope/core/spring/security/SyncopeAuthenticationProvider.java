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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.security.AuthContextUtils.Executable;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configurable
public class SyncopeAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeAuthenticationProvider.class);

    @Autowired
    protected AuthDataAccessor dataAccessor;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    protected String adminPassword;

    protected String adminPasswordAlgorithm;

    protected String anonymousKey;

    protected UserDetailsService userDetailsService;

    protected final Encryptor encryptor = Encryptor.getInstance();

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

    public void setUserDetailsService(final UserDetailsService syncopeUserDetailsService) {
        this.userDetailsService = syncopeUserDetailsService;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        String domainKey = SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain();
        if (StringUtils.isBlank(domainKey)) {
            domainKey = SyncopeConstants.MASTER_DOMAIN;
        }
        SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).setDomain(domainKey);

        Boolean authenticated;
        if (anonymousUser.equals(authentication.getName())) {
            authenticated = authentication.getCredentials().toString().equals(anonymousKey);
        } else if (adminUser.equals(authentication.getName())) {
            if (SyncopeConstants.MASTER_DOMAIN.equals(domainKey)) {
                authenticated = encryptor.verify(
                        authentication.getCredentials().toString(),
                        CipherAlgorithm.valueOf(adminPasswordAlgorithm),
                        adminPassword);
            } else {
                final String domainToFind = domainKey;
                authenticated = AuthContextUtils.execWithAuthContext(
                        SyncopeConstants.MASTER_DOMAIN, new Executable<Boolean>() {

                    @Override
                    public Boolean exec() {
                        Domain domain = dataAccessor.findDomain(domainToFind);

                        return encryptor.verify(
                                authentication.getCredentials().toString(),
                                domain.getAdminCipherAlgorithm(),
                                domain.getAdminPwd());
                    }
                });
            }
        } else {
            final Pair<String, Boolean> authResult =
                    AuthContextUtils.execWithAuthContext(domainKey, new Executable<Pair<String, Boolean>>() {

                        @Override
                        public Pair<String, Boolean> exec() {
                            return dataAccessor.authenticate(authentication);
                        }
                    });
            authenticated = authResult.getValue();
            if (authenticated != null && !authenticated) {
                AuthContextUtils.execWithAuthContext(domainKey, new Executable<Void>() {

                    @Override
                    public Void exec() {
                        provisioningManager.internalSuspend(authResult.getKey());
                        return null;
                    }
                });
            }
        }

        final boolean isAuthenticated = authenticated != null && authenticated;
        UsernamePasswordAuthenticationToken token;
        if (isAuthenticated) {
            token = AuthContextUtils.execWithAuthContext(
                    domainKey, new Executable<UsernamePasswordAuthenticationToken>() {

                @Override
                public UsernamePasswordAuthenticationToken exec() {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            authentication.getPrincipal(),
                            null,
                            userDetailsService.loadUserByUsername(authentication.getPrincipal().toString()).
                            getAuthorities());
                    token.setDetails(authentication.getDetails());

                    dataAccessor.audit(
                            AuditElements.EventCategoryType.REST,
                            AuditElements.AUTHENTICATION_CATEGORY,
                            null,
                            AuditElements.LOGIN_EVENT,
                            Result.SUCCESS,
                            null,
                            isAuthenticated,
                            authentication,
                            "Successfully authenticated, with entitlements: " + token.getAuthorities());
                    return token;
                }
            });

            LOG.debug("User {} successfully authenticated, with entitlements {}",
                    authentication.getPrincipal(), token.getAuthorities());
        } else {
            AuthContextUtils.execWithAuthContext(domainKey, new Executable<Void>() {

                @Override
                public Void exec() {
                    dataAccessor.audit(
                            AuditElements.EventCategoryType.REST,
                            AuditElements.AUTHENTICATION_CATEGORY,
                            null,
                            AuditElements.LOGIN_EVENT,
                            Result.FAILURE,
                            null,
                            isAuthenticated,
                            authentication,
                            "User " + authentication.getPrincipal() + " not authenticated");
                    return null;
                }
            });

            LOG.debug("User {} not authenticated", authentication.getPrincipal());

            throw new BadCredentialsException("User " + authentication.getPrincipal() + " not authenticated");
        }

        return token;
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
