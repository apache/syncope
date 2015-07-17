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
package org.apache.syncope.core.misc.security;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.misc.AuditManager;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.Realm;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

@Configurable
public class SyncopeAuthenticationProvider implements AuthenticationProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeAuthenticationProvider.class);

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected DomainDAO domainDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected ConnectorFactory connFactory;

    @Autowired
    protected AnyUtilsFactory attrUtilsFactory;

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
    @Transactional(noRollbackFor = { BadCredentialsException.class, DisabledException.class })
    public Authentication authenticate(final Authentication authentication) {
        boolean authenticated = false;

        String domainKey = authentication.getDetails() instanceof SyncopeAuthenticationDetails
                ? SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain()
                : null;
        if (StringUtils.isBlank(domainKey)) {
            domainKey = SyncopeConstants.MASTER_DOMAIN;
        }
        SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).setDomain(domainKey);

        if (anonymousUser.equals(authentication.getName())) {
            authenticated = authentication.getCredentials().toString().equals(anonymousKey);
        } else if (adminUser.equals(authentication.getName())) {
            if (SyncopeConstants.MASTER_DOMAIN.equals(domainKey)) {
                authenticated = encryptor.verify(
                        authentication.getCredentials().toString(),
                        CipherAlgorithm.valueOf(adminPasswordAlgorithm),
                        adminPassword);
            } else {
                Domain domain = domainDAO.find(domainKey);
                if (domain == null) {
                    throw new NotFoundException("Could not find domain " + domainKey);
                }

                authenticated = encryptor.verify(
                        authentication.getCredentials().toString(),
                        domain.getAdminCipherAlgorithm(),
                        domain.getAdminPwd());
            }
        } else {
            User user = userDAO.find(authentication.getName());

            if (user != null) {
                if (user.isSuspended() != null && user.isSuspended()) {
                    throw new DisabledException("User " + user.getUsername() + " is suspended");
                }

                CPlainAttr authStatuses = confDAO.find("authentication.statuses");
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
                    "Successfully authenticated, with groups: " + token.getAuthorities());

            LOG.debug("User {} successfully authenticated, with groups {}",
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

    protected void updateLoginAttributes(final User user, final boolean authenticated) {
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

    protected Set<? extends ExternalResource> getPassthroughResources(final User user) {
        Set<? extends ExternalResource> result = null;

        // 1. look for assigned resources, pick the ones whose account policy has authentication resources
        for (ExternalResource resource : userDAO.findAllResources(user)) {
            if (resource.getAccountPolicy() != null && !resource.getAccountPolicy().getResources().isEmpty()) {
                if (result == null) {
                    result = resource.getAccountPolicy().getResources();
                } else {
                    result.retainAll(resource.getAccountPolicy().getResources());
                }
            }
        }

        // 2. look for realms, pick the ones whose account policy has authentication resources
        for (Realm realm : realmDAO.findAncestors(user.getRealm())) {
            if (realm.getAccountPolicy() != null && !realm.getAccountPolicy().getResources().isEmpty()) {
                if (result == null) {
                    result = realm.getAccountPolicy().getResources();
                } else {
                    result.retainAll(realm.getAccountPolicy().getResources());
                }
            }
        }

        return SetUtils.emptyIfNull(result);
    }

    protected boolean authenticate(final User user, final String password) {
        boolean authenticated = encryptor.verify(password, user.getCipherAlgorithm(), user.getPassword());
        LOG.debug("{} authenticated on internal storage: {}", user.getUsername(), authenticated);

        for (Iterator<? extends ExternalResource> itor = getPassthroughResources(user).iterator();
                itor.hasNext() && !authenticated;) {

            ExternalResource resource = itor.next();
            String connObjectKey = null;
            try {
                connObjectKey = MappingUtils.getConnObjectKeyValue(user, resource.getProvision(anyTypeDAO.findUser()));
                Uid uid = connFactory.getConnector(resource).authenticate(connObjectKey, password, null);
                if (uid != null) {
                    authenticated = true;
                }
            } catch (Exception e) {
                LOG.debug("Could not authenticate {} on {}", user.getUsername(), resource.getKey(), e);
            }
            LOG.debug("{} authenticated on {} as {}: {}",
                    user.getUsername(), resource.getKey(), connObjectKey, authenticated);
        }

        return authenticated;
    }

    @Override
    public boolean supports(final Class<? extends Object> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }
}
