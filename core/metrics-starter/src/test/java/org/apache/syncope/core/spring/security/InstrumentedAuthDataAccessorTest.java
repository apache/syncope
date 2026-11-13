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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jwt.JWTClaimsSet;
import dev.samstevens.totp.code.CodeVerifier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

class InstrumentedAuthDataAccessorTest {

    private static final String ADMIN = "admin";

    private static final String BOB = "bob";

    private SecurityProperties securityProperties;

    private EncryptorManager encryptorManager;

    private Encryptor encryptor;

    private CodeVerifier totpCodeVerifier;

    private RealmSearchDAO realmSearchDAO;

    private UserDAO userDAO;

    private GroupDAO groupDAO;

    private AnySearchDAO anySearchDAO;

    private AccessTokenDAO accessTokenDAO;

    private ConfParamOps confParamOps;

    private RoleDAO roleDAO;

    private DelegationDAO delegationDAO;

    private ExternalResourceDAO resourceDAO;

    private ConnectorManager connectorManager;

    private AuditManager auditManager;

    private MappingManager mappingManager;

    private DefaultCredentialChecker credentialChecker;

    private SimpleMeterRegistry meterRegistry;

    private InstrumentedAuthDataAccessor authDataAccessor;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setJwtIssuer("ApacheSyncope");
        securityProperties.setAdminUser(ADMIN);
        securityProperties.setAnonymousUser("anonymous");

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        encryptorManager = mock(EncryptorManager.class);
        encryptor = mock(Encryptor.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        totpCodeVerifier = mock(CodeVerifier.class);
        realmSearchDAO = mock(RealmSearchDAO.class);
        userDAO = mock(UserDAO.class);
        groupDAO = mock(GroupDAO.class);
        anySearchDAO = mock(AnySearchDAO.class);
        accessTokenDAO = mock(AccessTokenDAO.class);
        confParamOps = mock(ConfParamOps.class);
        roleDAO = mock(RoleDAO.class);
        delegationDAO = mock(DelegationDAO.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        connectorManager = mock(ConnectorManager.class);
        auditManager = mock(AuditManager.class);
        mappingManager = mock(MappingManager.class);
        credentialChecker = mock(DefaultCredentialChecker.class);

        when(confParamOps.get(anyString(), eq(StandardConfParams.MFA_ENABLED), any(), eq(boolean.class))).
                thenReturn(false);
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_ATTRIBUTES), any(),
                eq(String[].class))).thenReturn(new String[] { "username" });
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(),
                eq(String[].class))).thenReturn(new String[] { "active" });
        when(confParamOps.get(anyString(), eq(StandardConfParams.LOG_LAST_LOGIN_DATE), any(), eq(boolean.class))).
                thenReturn(true);

        meterRegistry = new SimpleMeterRegistry();

        authDataAccessor = new InstrumentedAuthDataAccessor(
                securityProperties,
                encryptorManager,
                totpCodeVerifier,
                realmSearchDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                resourceDAO,
                connectorManager,
                auditManager,
                mappingManager,
                credentialChecker,
                List.of(),
                meterRegistry);
    }

    private Authentication usernamePassword(final String username, final String credentials) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getCredentials()).thenReturn(credentials);
        when(authentication.getDetails()).
                thenReturn(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        return authentication;
    }

    private JWTAuthentication jwt(
            final String subject, final String issuer, final String jwtID) {

        JWTClaimsSet claims = new JWTClaimsSet.Builder().
                issuer(issuer).
                subject(subject).
                jwtID(jwtID).
                issueTime(new Date()).
                build();
        return new JWTAuthentication(
                claims,
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
    }

    private User user(final String username) {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getKey()).thenReturn(username + "-key");
        when(user.getStatus()).thenReturn("active");
        when(user.isSuspended()).thenReturn(false);
        when(user.isMustChangePassword()).thenReturn(false);
        when(user.getFailedLogins()).thenReturn(0);
        when(user.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);
        when(user.getPassword()).thenReturn("encoded-password");

        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/");
        when(user.getRealm()).thenReturn(realm);

        return user;
    }

    private Role role(final String entitlement, final String realmPath) {
        Role role = mock(Role.class);
        when(role.getEntitlements()).thenReturn(Set.of(entitlement));

        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn(realmPath);
        doReturn(List.of(realm)).when(role).getRealms();
        return role;
    }

    @Test
    void successfulUsernamePasswordAuthenticationIncrementsSuccessCounter() {
        User user = user(BOB);
        doReturn(Optional.of(user)).when(userDAO).findByUsername(BOB);
        Role userRole = role(IdRepoEntitlement.USER_READ, SyncopeConstants.ROOT_REALM);
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);
        when(userDAO.findAllResources(user)).thenReturn(List.of());
        when(realmSearchDAO.findAncestors(any())).thenReturn(List.of());
        when(encryptor.verify(any(), any(), any())).thenReturn(true);

        authDataAccessor.authenticate(Optional.empty(), usernamePassword(BOB, "password"));

        assertEquals(
                1.0,
                meterRegistry.counter(
                        "syncope.auth.username.success.count", "realm", "/").count());
    }

    @Test
    void failedUsernamePasswordAuthenticationIncrementsFailureCounter() {
        User user = user(BOB);
        doReturn(Optional.of(user)).when(userDAO).findByUsername(BOB);
        when(userDAO.save(user)).thenReturn(user);
        when(userDAO.findAllResources(user)).thenReturn(List.of());
        when(realmSearchDAO.findAncestors(any())).thenReturn(List.of());
        when(encryptor.verify(any(), any(), any())).thenReturn(false);

        authDataAccessor.authenticate(Optional.empty(), usernamePassword(BOB, "wrong"));

        assertEquals(
                1.0,
                meterRegistry.counter(
                        "syncope.auth.username.failure.count", "realm", "/").count());
    }

    @Test
    void suspendedUserIncrementsDisabledCounter() {
        User user = user(BOB);
        when(user.isSuspended()).thenReturn(true);
        doReturn(Optional.of(user)).when(userDAO).findByUsername(BOB);

        assertThrows(
                DisabledException.class,
                () -> authDataAccessor.authenticate(Optional.empty(), usernamePassword(BOB, "password")));

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.auth.username.disabled.count").count());
    }

    @Test
    void successfulJWTAuthenticationIncrementsSuccessCounter() {
        User user = user(BOB);
        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        when(provider.resolve(any())).
                thenReturn(Optional.of(
                        new JWTSSOProvider.ResolvedClaims(
                                user, Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ)))));

        InstrumentedAuthDataAccessor accessor = new InstrumentedAuthDataAccessor(
                securityProperties,
                encryptorManager,
                totpCodeVerifier,
                realmSearchDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                resourceDAO,
                connectorManager,
                auditManager,
                mappingManager,
                credentialChecker,
                List.of(provider),
                meterRegistry);

        accessor.authenticate(jwt(BOB, "issuer", "jti"));

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.auth.jwt.success.count").count());
    }

    @Test
    void jwtWithUnknownIssuerIncrementsNotFoundCounter() {
        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        when(provider.resolve(any())).thenReturn(Optional.empty());

        InstrumentedAuthDataAccessor accessor = new InstrumentedAuthDataAccessor(
                securityProperties,
                encryptorManager,
                totpCodeVerifier,
                realmSearchDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                resourceDAO,
                connectorManager,
                auditManager,
                mappingManager,
                credentialChecker,
                List.of(provider),
                meterRegistry);

        assertThrows(
                org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> accessor.authenticate(jwt(BOB, "issuer", "jti")));

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.auth.jwt.notfound.count").count());
    }

    @Test
    void suspendedJWTUserIncrementsDisabledCounter() {
        User user = user(BOB);
        when(user.isSuspended()).thenReturn(true);
        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        when(provider.resolve(any())).
                thenReturn(Optional.of(new JWTSSOProvider.ResolvedClaims(user, Set.of())));

        InstrumentedAuthDataAccessor accessor = new InstrumentedAuthDataAccessor(
                securityProperties,
                encryptorManager,
                totpCodeVerifier,
                realmSearchDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                resourceDAO,
                connectorManager,
                auditManager,
                mappingManager,
                credentialChecker,
                List.of(provider),
                meterRegistry);

        assertThrows(
                DisabledException.class,
                () -> accessor.authenticate(jwt(BOB, "issuer", "jti")));

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.auth.jwt.disabled.count").count());
    }
}
