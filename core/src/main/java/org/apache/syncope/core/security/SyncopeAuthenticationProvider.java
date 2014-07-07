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

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.Encryptor;
import org.apache.syncope.core.util.MappingUtil;
import org.identityconnectors.framework.common.objects.Uid;
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
    private ConfDAO confDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ConnectorFactory connFactory;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    private String adminPassword;

    private String adminPasswordAlgorithm;

    private String anonymousKey;

    private SyncopeUserDetailsService userDetailsService;

    private final Encryptor encryptor = Encryptor.getInstance();

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

    public void setSyncopeUserDetailsService(final SyncopeUserDetailsService syncopeUserDetailsService) {
        this.userDetailsService = syncopeUserDetailsService;
    }

    @Override
    @Transactional(noRollbackFor = { BadCredentialsException.class, DisabledException.class })
    public Authentication authenticate(final Authentication authentication)
            throws AuthenticationException {

        boolean authenticated = false;
        SyncopeUser user = null;

        String username = authentication.getName();
        if (anonymousUser.equals(username)) {
            authenticated = authentication.getCredentials().toString().equals(anonymousKey);
        } else if (adminUser.equals(username)) {
            authenticated = encryptor.verify(
                    authentication.getCredentials().toString(),
                    CipherAlgorithm.valueOf(adminPasswordAlgorithm),
                    adminPassword);
        } else {
            user = userDAO.find(username);

            if (user != null) {
                if (user.isSuspended() != null && user.isSuspended()) {
                    throw new DisabledException("User " + user.getUsername() + " is suspended");
                }

                CAttr authStatuses = confDAO.find("authentication.statuses");
                if (authStatuses != null && !authStatuses.getValuesAsStrings().contains(user.getStatus())) {
                    throw new DisabledException("User " + user.getUsername() + " not allowed to authenticate");
                }

                authenticated = authenticate(user, authentication.getCredentials().toString());

                updateLoginAttributes(user, authenticated);
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
        } else {
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

    private void updateLoginAttributes(SyncopeUser user, boolean authenticated) {
        boolean userModified = false;

        if (authenticated) {
            if (confDAO.find("log.lastlogindate", Boolean.toString(true)).getValues().get(0).getBooleanValue()) {
                user.setLastLoginDate(new Date());
                userModified = true;
            }

            if (user.getFailedLogins() != 0) {
                user.setFailedLogins(0);
                userModified = true;
            }
        } else {
            user.setFailedLogins(user.getFailedLogins() + 1);
            userModified = true;
        }

        if (userModified) {
            userDAO.save(user);
        }
    }

    protected Set<ExternalResource> getPassthroughResources(final SyncopeUser user) {
        Set<ExternalResource> result = null;

        // 1. look for directly assigned resources, pick the ones whose account policy has authentication resources
        for (ExternalResource resource : user.getOwnResources()) {
            if (resource.getAccountPolicy() != null && !resource.getAccountPolicy().getResources().isEmpty()) {
                if (result == null) {
                    result = resource.getAccountPolicy().getResources();
                } else {
                    result.retainAll(resource.getAccountPolicy().getResources());
                }
            }
        }

        // 2. look for owned roles, pick the ones whose account policy has authentication resources
        for (SyncopeRole role : user.getRoles()) {
            if (role.getAccountPolicy() != null && !role.getAccountPolicy().getResources().isEmpty()) {
                if (result == null) {
                    result = role.getAccountPolicy().getResources();
                } else {
                    result.retainAll(role.getAccountPolicy().getResources());
                }
            }
        }

        // 3. look for global account policy (if defined)
        AccountPolicy global = policyDAO.getGlobalAccountPolicy();
        if (global != null && !global.getResources().isEmpty()) {
            if (result == null) {
                result = global.getResources();
            } else {
                result.retainAll(global.getResources());
            }
        }

        if (result == null) {
            result = Collections.emptySet();
        }

        return result;
    }

    protected boolean authenticate(final SyncopeUser user, final String password) {
        boolean authenticated = encryptor.verify(password, user.getCipherAlgorithm(), user.getPassword());
        LOG.debug("{} authenticated on internal storage: {}", user.getUsername(), authenticated);

        final AttributableUtil attrUtil = AttributableUtil.getInstance(AttributableType.USER);
        for (Iterator<ExternalResource> itor = getPassthroughResources(user).iterator();
                itor.hasNext() && !authenticated;) {

            ExternalResource resource = itor.next();
            String accountId = null;
            try {
                accountId = MappingUtil.getAccountIdValue(user, resource, attrUtil.getAccountIdItem(resource));
                Uid uid = connFactory.getConnector(resource).authenticate(accountId, password, null);
                if (uid != null) {
                    authenticated = true;
                }
            } catch (Exception e) {
                LOG.debug("Could not authenticate {} on {}", user.getUsername(), resource.getName(), e);
            }
            LOG.debug("{} authenticated on {} as {}: {}",
                    user.getUsername(), resource.getName(), accountId, authenticated);
        }

        return authenticated;
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
