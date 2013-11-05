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
package org.apache.syncope.core.security;

import java.util.Date;
import javax.annotation.Resource;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;

@Configurable
public class SyncopeAuthenticationProvider implements AuthenticationProvider {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeAuthenticationProvider.class);

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private UserDAO userDAO;

    @Resource(name = "adminUser")
    private String adminUser;

    private String adminPassword;

    private String adminPasswordAlgorithm;

    private SyncopeUserDetailsService userDetailsService;

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

    public void setSyncopeUserDetailsService(final SyncopeUserDetailsService syncopeUserDetailsService) {
        this.userDetailsService = syncopeUserDetailsService;
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, DisabledException.class})
    public Authentication authenticate(final Authentication authentication)
            throws AuthenticationException {

        boolean authenticated = false;
        SyncopeUser user = null;

        String username = authentication.getName();
        if (adminUser.equals(username)) {
            authenticated = authenticate(
                    authentication.getCredentials().toString(),
                    CipherAlgorithm.valueOf(adminPasswordAlgorithm),
                    adminPassword);
        } else {
            user = userDAO.find(username);

            if (user != null && user.isSuspended() != null) {
                if (user.isSuspended()) {
                    throw new DisabledException("User " + user.getUsername() + " is suspended");
                }
                authenticated = authenticate(
                        authentication.getCredentials().toString(),
                        user.getCipherAlgorithm(),
                        user.getPassword());
            }
        }

        UsernamePasswordAuthenticationToken token;

        if (authenticated) {
            token = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    null,
                    userDetailsService.loadUserByUsername(authentication.getPrincipal().toString()).getAuthorities());

            token.setDetails(authentication.getDetails());

            auditManager.audit(
                    AuditElements.EventCategoryType.REST,
                    "AuthenticationController",
                    null,
                    "login",
                    Result.SUCCESS,
                    null,
                    authenticated,
                    authentication,
                    "Successfully authenticated, with roles: " + token.getAuthorities());

            LOG.debug("User {} successfully authenticated, with roles {}",
                    authentication.getPrincipal(), token.getAuthorities());

            if (user != null) {
                user.setLastLoginDate(new Date());
                user.setFailedLogins(0);
                userDAO.save(user);
            }
        } else {
            if (user != null) {
                user.setFailedLogins(user.getFailedLogins() + 1);
                userDAO.save(user);
            }

            auditManager.audit(
                    AuditElements.EventCategoryType.REST,
                    "AuthenticationController",
                    null,
                    "login",
                    Result.FAILURE,
                    null,
                    authenticated,
                    authentication,
                    "User " + authentication.getPrincipal() + " not authenticated");

            LOG.debug("User {} not authenticated", authentication.getPrincipal());

            throw new BadCredentialsException("User " + authentication.getPrincipal() + " not authenticated");
        }

        return token;
    }

    protected boolean authenticate(final String password, final CipherAlgorithm cipherAlgorithm,
            final String digestedPassword) {

        return PasswordEncoder.verify(password, cipherAlgorithm, digestedPassword);
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
