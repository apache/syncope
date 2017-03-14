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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional} access to authentication / authorization data.
 *
 * @see SyncopeAuthenticationProvider
 * @see SyncopeAuthenticationDetails
 */
public class AuthDataAccessor {

    protected static final Logger LOG = LoggerFactory.getLogger(AuthDataAccessor.class);

    protected static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    protected static final Set<SyncopeGrantedAuthority> ANONYMOUS_AUTHORITIES =
            Collections.singleton(new SyncopeGrantedAuthority(StandardEntitlement.ANONYMOUS));

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected DomainDAO domainDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected AccessTokenDAO accessTokenDAO;

    @Autowired
    protected ConnectorFactory connFactory;

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected MappingManager mappingManager;

    @Transactional(readOnly = true)
    public Domain findDomain(final String key) {
        Domain domain = domainDAO.find(key);
        if (domain == null) {
            throw new AuthenticationServiceException("Could not find domain " + key);
        }
        return domain;
    }

    /**
     * Attempts to authenticate the given credentials against internal storage and pass-through resources (if
     * configured): the first succeeding causes global success.
     *
     * @param authentication given credentials
     * @return {@code null} if no matching user was found, authentication result otherwise
     */
    @Transactional(noRollbackFor = DisabledException.class)
    public Pair<User, Boolean> authenticate(final Authentication authentication) {
        User user = null;

        CPlainAttr authAttrs = confDAO.find("authentication.attributes");
        List<String> authAttrValues = authAttrs == null
                ? Collections.singletonList("username")
                : authAttrs.getValuesAsStrings();
        for (int i = 0; user == null && i < authAttrValues.size(); i++) {
            if ("username".equals(authAttrValues.get(i))) {
                user = userDAO.findByUsername(authentication.getName());
            } else {
                AttributeCond attrCond = new AttributeCond(AttributeCond.Type.EQ);
                attrCond.setSchema(authAttrValues.get(i));
                attrCond.setExpression(authentication.getName());
                List<User> users = searchDAO.search(SearchCond.getLeafCond(attrCond), AnyTypeKind.USER);
                if (users.size() == 1) {
                    user = users.get(0);
                } else {
                    LOG.warn("Value {} provided for {} does not uniquely identifies an user",
                            authentication.getName(), authAttrValues.get(i));
                }
            }
        }

        Boolean authenticated = null;
        if (user != null) {
            authenticated = false;

            if (user.isSuspended() != null && user.isSuspended()) {
                throw new DisabledException("User " + user.getUsername() + " is suspended");
            }

            CPlainAttr authStatuses = confDAO.find("authentication.statuses");
            if (authStatuses != null && !authStatuses.getValuesAsStrings().contains(user.getStatus())) {
                throw new DisabledException("User " + user.getUsername() + " not allowed to authenticate");
            }

            boolean userModified = false;
            authenticated = AuthDataAccessor.this.authenticate(user, authentication.getCredentials().toString());
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

        return ImmutablePair.of(user, authenticated);
    }

    protected boolean authenticate(final User user, final String password) {
        boolean authenticated = ENCRYPTOR.verify(password, user.getCipherAlgorithm(), user.getPassword());
        LOG.debug("{} authenticated on internal storage: {}", user.getUsername(), authenticated);

        for (Iterator<? extends ExternalResource> itor = getPassthroughResources(user).iterator();
                itor.hasNext() && !authenticated;) {

            ExternalResource resource = itor.next();
            String connObjectKey = null;
            try {
                connObjectKey = mappingManager.getConnObjectKeyValue(
                        user, resource.getProvision(anyTypeDAO.findUser()));
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

    protected Set<SyncopeGrantedAuthority> getAdminAuthorities() {
        return CollectionUtils.collect(EntitlementsHolder.getInstance().getValues(),
                new Transformer<String, SyncopeGrantedAuthority>() {

            @Override
            public SyncopeGrantedAuthority transform(final String entitlement) {
                return new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM);
            }
        }, new HashSet<SyncopeGrantedAuthority>());
    }

    protected Set<SyncopeGrantedAuthority> getUserAuthorities(final User user) {
        Set<SyncopeGrantedAuthority> authorities = new HashSet<>();

        if (user.isMustChangePassword()) {
            authorities.add(new SyncopeGrantedAuthority(StandardEntitlement.MUST_CHANGE_PASSWORD));
        } else {
            final Map<String, Set<String>> entForRealms = new HashMap<>();

            // Give entitlements as assigned by roles (with realms, where applicable) - assigned either
            // statically and dynamically
            for (final Role role : userDAO.findAllRoles(user)) {
                IterableUtils.forEach(role.getEntitlements(), new Closure<String>() {

                    @Override
                    public void execute(final String entitlement) {
                        Set<String> realms = entForRealms.get(entitlement);
                        if (realms == null) {
                            realms = new HashSet<>();
                            entForRealms.put(entitlement, realms);
                        }

                        CollectionUtils.collect(role.getRealms(), new Transformer<Realm, String>() {

                            @Override
                            public String transform(final Realm realm) {
                                return realm.getFullPath();
                            }
                        }, realms);
                    }
                });
            }

            // Give group entitlements for owned groups
            for (Group group : groupDAO.findOwnedByUser(user.getKey())) {
                for (String entitlement : Arrays.asList(
                        StandardEntitlement.GROUP_READ,
                        StandardEntitlement.GROUP_UPDATE,
                        StandardEntitlement.GROUP_DELETE)) {

                    Set<String> realms = entForRealms.get(entitlement);
                    if (realms == null) {
                        realms = new HashSet<>();
                        entForRealms.put(entitlement, realms);
                    }

                    realms.add(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
                }
            }

            // Finally normalize realms for each given entitlement and generate authorities
            for (Map.Entry<String, Set<String>> entry : entForRealms.entrySet()) {
                SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(entry.getKey());
                authority.addRealms(RealmUtils.normalize(entry.getValue()));
                authorities.add(authority);
            }
        }

        return authorities;
    }

    @Transactional
    public Set<SyncopeGrantedAuthority> getAuthorities(final String username) {
        Set<SyncopeGrantedAuthority> authorities;

        if (anonymousUser.equals(username)) {
            authorities = ANONYMOUS_AUTHORITIES;
        } else if (adminUser.equals(username)) {
            authorities = getAdminAuthorities();
        } else {
            User user = userDAO.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("Could not find any user with id " + username);
            }

            authorities = getUserAuthorities(user);
        }

        return authorities;
    }

    @Transactional
    public Set<SyncopeGrantedAuthority> authenticate(final JWTAuthentication authentication) {
        AccessToken accessToken = accessTokenDAO.find(authentication.getClaims().getTokenId());
        if (accessToken == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Could not find JWT " + authentication.getClaims().getTokenId());
        }

        Set<SyncopeGrantedAuthority> authorities;
        if (adminUser.equals(accessToken.getOwner())) {
            authorities = getAdminAuthorities();
        } else {
            User user = userDAO.findByUsername(accessToken.getOwner());
            if (user == null) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Could not find user " + accessToken.getOwner()
                        + " for JWT " + authentication.getClaims().getTokenId());
            }

            if (user.isSuspended() != null && user.isSuspended()) {
                throw new DisabledException("User " + user.getUsername() + " is suspended");
            }

            CPlainAttr authStatuses = confDAO.find("authentication.statuses");
            if (authStatuses != null && !authStatuses.getValuesAsStrings().contains(user.getStatus())) {
                throw new DisabledException("User " + user.getUsername() + " not allowed to authenticate");
            }

            authorities = getUserAuthorities(user);
        }

        return authorities;
    }

    @Transactional
    public void removeExpired(final String tokenKey) {
        accessTokenDAO.delete(tokenKey);
    }

    @Transactional(readOnly = true)
    public void audit(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final AuditElements.Result result,
            final Object before,
            final Object output,
            final Object... input) {

        auditManager.audit(type, category, subcategory, event, result, before, output, input);
    }

}
