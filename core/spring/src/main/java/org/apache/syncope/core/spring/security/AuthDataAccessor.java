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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.security.auth.login.AccountNotFoundException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional}) access to authentication / authorization data.
 *
 * @see JWTAuthenticationFilter
 * @see UsernamePasswordAuthenticationProvider
 * @see SyncopeAuthenticationDetails
 */
public class AuthDataAccessor {

    protected static final Logger LOG = LoggerFactory.getLogger(AuthDataAccessor.class);

    protected static final Set<SyncopeGrantedAuthority> ANONYMOUS_AUTHORITIES =
            Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.ANONYMOUS));

    protected static final Set<SyncopeGrantedAuthority> MUST_CHANGE_PASSWORD_AUTHORITIES =
            Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.MUST_CHANGE_PASSWORD));

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    protected final RealmSearchDAO realmSearchDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final ConfParamOps confParamOps;

    protected final RoleDAO roleDAO;

    protected final DelegationDAO delegationDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final ConnectorManager connectorManager;

    protected final AuditManager auditManager;

    protected final MappingManager mappingManager;

    private final List<JWTSSOProvider> jwtSSOProviders;

    public AuthDataAccessor(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final RealmSearchDAO realmSearchDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AccessTokenDAO accessTokenDAO,
            final ConfParamOps confParamOps,
            final RoleDAO roleDAO,
            final DelegationDAO delegationDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnectorManager connectorManager,
            final AuditManager auditManager,
            final MappingManager mappingManager,
            final List<JWTSSOProvider> jwtSSOProviders) {

        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
        this.realmSearchDAO = realmSearchDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anySearchDAO = anySearchDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.confParamOps = confParamOps;
        this.roleDAO = roleDAO;
        this.delegationDAO = delegationDAO;
        this.resourceDAO = resourceDAO;
        this.connectorManager = connectorManager;
        this.auditManager = auditManager;
        this.mappingManager = mappingManager;
        this.jwtSSOProviders = jwtSSOProviders;
    }

    public JWTSSOProvider getJWTSSOProvider(final String issuer) {
        if (issuer == null) {
            throw new AuthenticationCredentialsNotFoundException("A null issuer is not permitted");
        }

        return jwtSSOProviders.stream().filter(provider -> issuer.equals(provider.getIssuer())).findFirst().
                orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                "Could not find any registered JWTSSOProvider for issuer " + issuer));
    }

    protected String getDelegationKey(final SyncopeAuthenticationDetails details, final String delegatedKey) {
        if (details.getDelegatedBy() == null) {
            return null;
        }

        String delegatingKey = SyncopeConstants.UUID_PATTERN.matcher(details.getDelegatedBy()).matches()
                ? details.getDelegatedBy()
                : userDAO.findKey(details.getDelegatedBy()).orElse(null);
        if (delegatingKey == null) {
            throw new SessionAuthenticationException(
                    "Delegating user " + details.getDelegatedBy() + " cannot be found");
        }

        LOG.debug("Delegation request: delegating:{}, delegated:{}", delegatingKey, delegatedKey);

        return delegationDAO.findValidFor(delegatingKey, delegatedKey, OffsetDateTime.now()).
                orElseThrow(() -> new SessionAuthenticationException(
                "Delegation by " + delegatingKey + " was requested but none found"));
    }

    /**
     * Attempts to authenticate the given credentials against internal storage and pass-through resources (if
     * configured): the first succeeding causes global success.
     *
     * @param domain domain
     * @param authentication given credentials
     * @return {@code null} if no matching user was found, authentication result otherwise
     */
    @Transactional(noRollbackFor = DisabledException.class)
    public Triple<User, Boolean, String> authenticate(final String domain, final Authentication authentication) {
        User user = null;

        String[] authAttrValues = confParamOps.get(
                domain, "authentication.attributes", new String[] { "username" }, String[].class);
        for (int i = 0; user == null && i < authAttrValues.length; i++) {
            if ("username".equals(authAttrValues[i])) {
                user = userDAO.findByUsername(authentication.getName()).orElse(null);
            } else {
                AttrCond attrCond = new AttrCond(AttrCond.Type.EQ);
                attrCond.setSchema(authAttrValues[i]);
                attrCond.setExpression(authentication.getName());
                try {
                    List<User> users = anySearchDAO.search(SearchCond.of(attrCond), AnyTypeKind.USER);
                    if (users.size() == 1) {
                        user = users.getFirst();
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

            String[] authStatuses = confParamOps.get(
                    domain, "authentication.statuses", new String[] {}, String[].class);
            if (!ArrayUtils.contains(authStatuses, user.getStatus())) {
                throw new DisabledException("User " + user.getUsername() + " not allowed to authenticate");
            }

            boolean userModified = false;
            authenticated = authenticate(user, authentication.getCredentials().toString());
            if (authenticated) {
                delegationKey = getDelegationKey(
                        SyncopeAuthenticationDetails.class.cast(authentication.getDetails()), user.getKey());

                if (confParamOps.get(domain, "log.lastlogindate", true, Boolean.class)) {
                    user.setLastLoginDate(OffsetDateTime.now());
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
        boolean authenticated = encryptorManager.getInstance().
                verify(password, user.getCipherAlgorithm(), user.getPassword());
        LOG.debug("{} authenticated on internal storage: {}", user.getUsername(), authenticated);

        for (Iterator<? extends ExternalResource> itor = getPassthroughResources(user).iterator();
                itor.hasNext() && !authenticated;) {

            ExternalResource resource = itor.next();
            String connObjectKey = null;
            try {
                Provision provision = resource.getProvisionByAnyType(AnyTypeKind.USER.name()).
                        orElseThrow(() -> new AccountNotFoundException(
                        "Unable to locate provision for user type " + AnyTypeKind.USER.name()));
                connObjectKey = mappingManager.getConnObjectKeyValue(user, resource, provision).
                        orElseThrow(() -> new AccountNotFoundException(
                        "Unable to locate conn object key value for " + AnyTypeKind.USER.name()));
                Uid uid = connectorManager.getConnector(resource).authenticate(connObjectKey, password, null);
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

    protected Set<ExternalResource> getPassthroughResources(final User user) {
        Set<ExternalResource> result = new HashSet<>();

        // 1. look for assigned resources, pick the ones whose account policy has authentication resources
        userDAO.findAllResources(user).stream().
                filter(r -> r.getAccountPolicy() != null).forEach(resource -> {

            if (result.isEmpty()) {
                result.addAll(resourceDAO.findByPolicy(resource.getAccountPolicy()));
            } else {
                result.retainAll(resourceDAO.findByPolicy(resource.getAccountPolicy()));
            }
        });

        // 2. look for realms, pick the ones whose account policy has authentication resources
        realmSearchDAO.findAncestors(user.getRealm()).stream().
                filter(r -> r.getAccountPolicy() != null).forEach(realm -> {

            if (result.isEmpty()) {
                result.addAll(resourceDAO.findByPolicy(realm.getAccountPolicy()));
            } else {
                result.retainAll(resourceDAO.findByPolicy(realm.getAccountPolicy()));
            }
        });

        return result;
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
            return MUST_CHANGE_PASSWORD_AUTHORITIES;
        }

        Map<String, Set<String>> entForRealms = new HashMap<>();

        // Give entitlements as assigned by roles (with static or dynamic realms, where applicable) - assigned
        // either statically and dynamically
        userDAO.findAllRoles(user).stream().
                filter(role -> !RoleDAO.GROUP_OWNER_ROLE.equals(role.getKey())).
                forEach(role -> role.getEntitlements().forEach(entitlement -> {
            Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                Set<String> r = new HashSet<>();
                entForRealms.put(entitlement, r);
                return r;
            });

            realms.addAll(role.getRealms().stream().map(Realm::getFullPath).collect(Collectors.toSet()));
            if (!entitlement.endsWith("_CREATE") && !entitlement.endsWith("_DELETE")) {
                realms.addAll(role.getDynRealms().stream().map(DynRealm::getKey).toList());
            }
        }));

        // Give group entitlements for owned groups
        groupDAO.findOwnedByUser(user.getKey()).
                forEach(group -> roleDAO.findById(RoleDAO.GROUP_OWNER_ROLE).ifPresentOrElse(
                groupOwnerRole -> groupOwnerRole.getEntitlements().forEach(entitlement -> {
                    Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                        HashSet<String> r = new HashSet<>();
                        entForRealms.put(entitlement, r);
                        return r;
                    });

                    realms.add(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
                }),
                () -> LOG.warn("Role {} was not found", RoleDAO.GROUP_OWNER_ROLE)));

        return buildAuthorities(entForRealms);
    }

    protected Set<SyncopeGrantedAuthority> getDelegatedAuthorities(final Delegation delegation) {
        Map<String, Set<String>> entForRealms = new HashMap<>();

        delegation.getRoles().stream().filter(role -> !RoleDAO.GROUP_OWNER_ROLE.equals(role.getKey())).
                forEach(role -> role.getEntitlements().forEach(entitlement -> {
            Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                HashSet<String> r = new HashSet<>();
                entForRealms.put(entitlement, r);
                return r;
            });

            realms.addAll(role.getRealms().stream().map(Realm::getFullPath).collect(Collectors.toSet()));
            if (!entitlement.endsWith("_CREATE") && !entitlement.endsWith("_DELETE")) {
                realms.addAll(role.getDynRealms().stream().map(DynRealm::getKey).toList());
            }
        }));

        return buildAuthorities(entForRealms);
    }

    @Transactional
    public Set<SyncopeGrantedAuthority> getAuthorities(final String username, final String delegationKey) {
        Set<SyncopeGrantedAuthority> authorities;

        if (securityProperties.getAnonymousUser().equals(username)) {
            authorities = ANONYMOUS_AUTHORITIES;
        } else if (securityProperties.getAdminUser().equals(username)) {
            authorities = getAdminAuthorities();
        } else if (delegationKey != null) {
            Delegation delegation = delegationDAO.findById(delegationKey).
                    orElseThrow(() -> new UsernameNotFoundException(
                    "Could not find delegation " + delegationKey));

            authorities = delegation.getRoles().isEmpty()
                    ? getUserAuthorities(delegation.getDelegating())
                    : getDelegatedAuthorities(delegation);
        } else {
            User user = userDAO.findByUsername(username).
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

        if (securityProperties.getAdminUser().equals(authentication.getClaims().getSubject())) {
            accessTokenDAO.findById(authentication.getClaims().getJWTID()).
                    orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                    "Could not find an Access Token for JWT " + authentication.getClaims().getJWTID()));

            username = securityProperties.getAdminUser();
            authorities = getAdminAuthorities();
        } else {
            JWTSSOProvider jwtSSOProvider = getJWTSSOProvider(authentication.getClaims().getIssuer());
            Pair<User, Set<SyncopeGrantedAuthority>> resolved = jwtSSOProvider.resolve(authentication.getClaims());
            if (resolved == null || resolved.getLeft() == null) {
                throw new AuthenticationCredentialsNotFoundException(
                        "Could not find User " + authentication.getClaims().getSubject()
                        + " for JWT " + authentication.getClaims().getJWTID());
            }

            User user = resolved.getLeft();
            String delegationKey = getDelegationKey(authentication.getDetails(), user.getKey());
            username = user.getUsername();
            authorities = resolved.getRight() == null
                    ? Set.of()
                    : delegationKey == null
                            ? resolved.getRight()
                            : getAuthorities(username, delegationKey);
            LOG.debug("JWT {} issued by {} resolved to User {} with authorities {}",
                    authentication.getClaims().getJWTID(),
                    authentication.getClaims().getIssuer(),
                    username + Optional.ofNullable(delegationKey).
                            map(d -> " [under delegation " + delegationKey + "]").orElse(StringUtils.EMPTY),
                    authorities);

            if (BooleanUtils.isTrue(user.isSuspended())) {
                throw new DisabledException("User " + username + " is suspended");
            }

            List<String> authStatuses = List.of(confParamOps.get(authentication.getDetails().getDomain(),
                    "authentication.statuses", new String[] {}, String[].class));
            if (!authStatuses.contains(user.getStatus())) {
                throw new DisabledException("User " + username + " not allowed to authenticate");
            }

            if (BooleanUtils.isTrue(user.isMustChangePassword())) {
                LOG.debug("User {} must change password, resetting authorities", username);
                authorities = MUST_CHANGE_PASSWORD_AUTHORITIES;
            }
        }

        return Pair.of(username, authorities);
    }

    @Transactional
    public void removeExpired(final String tokenKey) {
        accessTokenDAO.deleteById(tokenKey);
    }

    @Transactional(readOnly = true)
    public void audit(
            final String domain,
            final String username,
            final String delegationKey,
            final OpEvent.Outcome outcome,
            final Object output,
            final Object... input) {

        auditManager.audit(
                domain,
                username + Optional.ofNullable(delegationKey).
                        map(d -> " [under delegation " + delegationKey + "]").orElse(StringUtils.EMPTY),
                OpEvent.CategoryType.LOGIC,
                OpEvent.AUTHENTICATION_CATEGORY,
                null,
                OpEvent.LOGIN_OP,
                outcome,
                null,
                output,
                input);
    }
}
