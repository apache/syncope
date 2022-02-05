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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.security.auth.login.AccountNotFoundException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional}) access to authentication / authorization data.
 *
 * @see JWTAuthenticationProvider
 * @see UsernamePasswordAuthenticationProvider
 * @see SyncopeAuthenticationDetails
 */
public class AuthDataAccessor {

    public static final String GROUP_OWNER_ROLE = "GROUP_OWNER";

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
    protected RoleDAO roleDAO;

    @Autowired
    protected DelegationDAO delegationDAO;

    @Autowired
    protected ConnectorFactory connFactory;

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected MappingManager mappingManager;

    @Autowired
    protected ImplementationLookup implementationLookup;

    private Map<String, JWTSSOProvider> jwtSSOProviders;

    public JWTSSOProvider getJWTSSOProvider(final String issuer) {
        synchronized (this) {
            if (jwtSSOProviders == null) {
                jwtSSOProviders = new HashMap<>();

                implementationLookup.getJWTSSOProviderClasses().stream().
                        map(clazz -> (JWTSSOProvider) ApplicationContextProvider.getBeanFactory().
                        createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true)).
                        forEach(jwtSSOProvider -> jwtSSOProviders.put(jwtSSOProvider.getIssuer(), jwtSSOProvider));
            }
        }

        if (issuer == null) {
            throw new AuthenticationCredentialsNotFoundException("A null issuer is not permitted");
        }
        JWTSSOProvider provider = jwtSSOProviders.get(issuer);
        if (provider == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Could not find any registered JWTSSOProvider for issuer " + issuer);
        }

        return provider;
    }

    @Transactional(readOnly = true)
    public Domain findDomain(final String key) {
        Domain domain = domainDAO.find(key);
        if (domain == null) {
            throw new AuthenticationServiceException("Could not find domain " + key);
        }
        return domain;
    }

    protected String getDelegationKey(final SyncopeAuthenticationDetails details, final String delegatedKey) {
        if (details.getDelegatedBy() == null) {
            return null;
        }

        String delegatingKey = SyncopeConstants.UUID_PATTERN.matcher(details.getDelegatedBy()).matches()
                ? details.getDelegatedBy()
                : userDAO.findKey(details.getDelegatedBy());
        if (delegatingKey == null) {
            throw new SessionAuthenticationException(
                    "Delegating user " + details.getDelegatedBy() + " cannot be found");
        }

        LOG.debug("Delegation request: delegating:{}, delegated:{}", delegatingKey, delegatedKey);

        return delegationDAO.findValidFor(delegatingKey, delegatedKey).
                orElseThrow(() -> new SessionAuthenticationException(
                "Delegation by " + delegatingKey + " was requested but none found"));
    }

    /**
     * Attempts to authenticate the given credentials against internal storage and pass-through resources (if
     * configured): the first succeeding causes global success.
     *
     * @param authentication given credentials
     * @return {@code null} if no matching user was found, authentication result otherwise
     */
    @Transactional(noRollbackFor = DisabledException.class)
    public Triple<User, Boolean, String> authenticate(final Authentication authentication) {
        User user = null;

        List<String> authAttrValues = confDAO.find("authentication.attributes").
                map(CPlainAttr::getValuesAsStrings).
                orElseGet(() -> Collections.singletonList("username"));
        for (int i = 0; user == null && i < authAttrValues.size(); i++) {
            if ("username".equals(authAttrValues.get(i))) {
                user = userDAO.findByUsername(authentication.getName());
            } else {
                AttrCond attrCond = new AttrCond(AttrCond.Type.EQ);
                attrCond.setSchema(authAttrValues.get(i));
                attrCond.setExpression(authentication.getName());
                try {
                    List<User> users = searchDAO.search(SearchCond.getLeaf(attrCond), AnyTypeKind.USER);
                    if (users.size() == 1) {
                        user = users.get(0);
                    } else {
                        LOG.warn("Search condition {} does not uniquely match a user", attrCond);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.error("While searching user for authentication via {}", attrCond, e);
                }
            }
        }

        Boolean authenticated = null;
        String delegationKey = null;
        if (user != null) {
            authenticated = false;

            if (user.isSuspended() != null && user.isSuspended()) {
                throw new DisabledException("User " + user.getUsername() + " is suspended");
            }

            if (!confDAO.getValuesAsStrings("authentication.statuses").contains(user.getStatus())) {
                throw new DisabledException("User " + user.getUsername() + " not allowed to authenticate");
            }

            boolean userModified = false;
            authenticated = authenticate(user, authentication.getCredentials().toString());
            if (authenticated) {
                delegationKey = getDelegationKey(
                        SyncopeAuthenticationDetails.class.cast(authentication.getDetails()), user.getKey());

                if (confDAO.find("log.lastlogindate", true)) {
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

        return Triple.of(user, authenticated, delegationKey);
    }

    protected boolean authenticate(final User user, final String password) {
        boolean authenticated = ENCRYPTOR.verify(password, user.getCipherAlgorithm(), user.getPassword());
        LOG.debug("{} authenticated on internal storage: {}", user.getUsername(), authenticated);

        for (Iterator<? extends ExternalResource> itor = getPassthroughResources(user).iterator();
                itor.hasNext() && !authenticated;) {

            ExternalResource resource = itor.next();
            String connObjectKey = null;
            try {
                AnyType userType = anyTypeDAO.findUser();
                Provision provision = resource.getProvision(userType).
                        orElseThrow(() -> new AccountNotFoundException(
                        "Unable to locate provision for user type " + userType.getKey()));
                connObjectKey = mappingManager.getConnObjectKeyValue(user, provision).
                        orElseThrow(() -> new AccountNotFoundException(
                        "Unable to locate conn object key value for " + userType.getKey()));
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

        return result == null ? Collections.emptySet() : result;
    }

    protected Set<SyncopeGrantedAuthority> getAdminAuthorities() {
        return EntitlementsHolder.getInstance().getValues().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toSet());
    }

    protected Set<SyncopeGrantedAuthority> buildAuthorities(final Map<String, Set<String>> entForRealms) {
        Set<SyncopeGrantedAuthority> authorities = new HashSet<>();

        entForRealms.forEach((entitlement, realms) -> {
            Pair<Set<String>, Set<String>> normalized = RealmUtils.normalize(realms);

            SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(entitlement);
            authority.addRealms(normalized.getLeft());
            authority.addRealms(normalized.getRight());
            authorities.add(authority);
        });

        return authorities;
    }

    protected Set<SyncopeGrantedAuthority> getUserAuthorities(final User user) {
        if (user.isMustChangePassword()) {
            return Collections.singleton(new SyncopeGrantedAuthority(StandardEntitlement.MUST_CHANGE_PASSWORD));
        }

        Map<String, Set<String>> entForRealms = new HashMap<>();

        // Give entitlements as assigned by roles (with static or dynamic realms, where applicable) - assigned
        // either statically and dynamically
        userDAO.findAllRoles(user).stream().
                filter(role -> !GROUP_OWNER_ROLE.equals(role.getKey())).
                forEach(role -> role.getEntitlements().forEach(entitlement -> {
            Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                HashSet<String> r = new HashSet<>();
                entForRealms.put(entitlement, r);
                return r;
            });

            realms.addAll(role.getRealms().stream().map(Realm::getFullPath).collect(Collectors.toSet()));
            if (!entitlement.endsWith("_CREATE") && !entitlement.endsWith("_DELETE")) {
                realms.addAll(role.getDynRealms().stream().map(DynRealm::getKey).collect(Collectors.toList()));
            }
        }));

        // Give group entitlements for owned groups
        groupDAO.findOwnedByUser(user.getKey()).forEach(group -> {
            Role groupOwnerRole = roleDAO.find(GROUP_OWNER_ROLE);
            if (groupOwnerRole == null) {
                LOG.warn("Role {} was not found", GROUP_OWNER_ROLE);
            } else {
                groupOwnerRole.getEntitlements().forEach(entitlement -> {
                    Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                        HashSet<String> r = new HashSet<>();
                        entForRealms.put(entitlement, r);
                        return r;
                    });

                    realms.add(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
                });
            }
        });

        return buildAuthorities(entForRealms);
    }

    protected Set<SyncopeGrantedAuthority> getDelegatedAuthorities(final Delegation delegation) {
        Map<String, Set<String>> entForRealms = new HashMap<>();

        delegation.getRoles().stream().filter(role -> !GROUP_OWNER_ROLE.equals(role.getKey())).
                forEach(role -> role.getEntitlements().forEach(entitlement -> {
            Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                HashSet<String> r = new HashSet<>();
                entForRealms.put(entitlement, r);
                return r;
            });

            realms.addAll(role.getRealms().stream().map(Realm::getFullPath).collect(Collectors.toSet()));
            if (!entitlement.endsWith("_CREATE") && !entitlement.endsWith("_DELETE")) {
                realms.addAll(role.getDynRealms().stream().map(DynRealm::getKey).collect(Collectors.toList()));
            }
        }));

        return buildAuthorities(entForRealms);
    }

    @Transactional
    public Set<SyncopeGrantedAuthority> getAuthorities(final String username, final String delegationKey) {
        Set<SyncopeGrantedAuthority> authorities;

        if (anonymousUser.equals(username)) {
            authorities = ANONYMOUS_AUTHORITIES;
        } else if (adminUser.equals(username)) {
            authorities = getAdminAuthorities();
        } else if (delegationKey != null) {
            Delegation delegation = Optional.ofNullable(delegationDAO.find(delegationKey)).
                    orElseThrow(() -> new UsernameNotFoundException(
                    "Could not find delegation " + delegationKey));

            authorities = delegation.getRoles().isEmpty()
                    ? getUserAuthorities(delegation.getDelegating())
                    : getDelegatedAuthorities(delegation);
        } else {
            User user = Optional.ofNullable(userDAO.findByUsername(username)).
                    orElseThrow(() -> new UsernameNotFoundException(
                    "Could not find any user with username " + username));

            authorities = getUserAuthorities(user);
        }

        return authorities;
    }

    @Transactional
    public Pair<String, Set<SyncopeGrantedAuthority>> authenticate(final JWTAuthentication authentication) {
        String username;
        Set<SyncopeGrantedAuthority> authorities;

        if (adminUser.equals(authentication.getClaims().getSubject())) {
            AccessToken accessToken = accessTokenDAO.find(authentication.getClaims().getTokenId());
            if (accessToken == null) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Could not find an Access Token for JWT " + authentication.getClaims().getTokenId());
            }

            username = adminUser;
            authorities = getAdminAuthorities();
        } else {
            JWTSSOProvider jwtSSOProvider = getJWTSSOProvider(authentication.getClaims().getIssuer());
            Pair<User, Set<SyncopeGrantedAuthority>> resolved = jwtSSOProvider.resolve(authentication.getClaims());
            if (resolved == null || resolved.getLeft() == null) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Could not find User " + authentication.getClaims().getSubject()
                        + " for JWT " + authentication.getClaims().getTokenId());
            }

            User user = resolved.getLeft();
            String delegationKey = getDelegationKey(authentication.getDetails(), user.getKey());
            username = user.getUsername();
            authorities = resolved.getRight() == null
                    ? Collections.emptySet()
                    : delegationKey == null
                            ? resolved.getRight()
                            : getAuthorities(username, delegationKey);
            LOG.debug("JWT {} issued by {} resolved to User {} with authorities {}",
                    authentication.getClaims().getTokenId(),
                    authentication.getClaims().getIssuer(),
                    username + Optional.ofNullable(delegationKey).
                            map(d -> " [under delegation " + delegationKey + "]").orElse(StringUtils.EMPTY),
                    authorities);

            if (BooleanUtils.isTrue(user.isSuspended())) {
                throw new DisabledException("User " + username + " is suspended");
            }

            if (!confDAO.getValuesAsStrings("authentication.statuses").contains(user.getStatus())) {
                throw new DisabledException("User " + username + " not allowed to authenticate");
            }

            if (BooleanUtils.isTrue(user.isMustChangePassword())) {
                LOG.debug("User {} must change password, resetting authorities", username);
                authorities = Collections.singleton(
                        new SyncopeGrantedAuthority(StandardEntitlement.MUST_CHANGE_PASSWORD));
            }
        }

        return Pair.of(username, authorities);
    }

    @Transactional
    public void removeExpired(final String tokenKey) {
        accessTokenDAO.delete(tokenKey);
    }

    @Transactional(readOnly = true)
    public void audit(
            final String username,
            final String delegationKey,
            final AuditElements.Result result,
            final Object output,
            final Object... input) {

        auditManager.audit(
                username + Optional.ofNullable(delegationKey).
                        map(d -> " [under delegation " + delegationKey + "]").orElse(StringUtils.EMPTY),
                AuditElements.EventCategoryType.LOGIC, AuditElements.AUTHENTICATION_CATEGORY, null,
                AuditElements.LOGIN_EVENT, result, null, output, input);
    }
}
