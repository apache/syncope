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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.samstevens.totp.code.CodeVerifier;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.OpEvent;
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
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

class AuthDataAccessorTest {

    private static final String LOW_PRIVILEGE_JTI = "low-privilege-users-valid-token-id";

    private static final String LOW_PRIVILEGE_USER = "low-priv-user";

    private static final String ADMIN = "admin";

    private static final String ANONYMOUS = "anonymous";

    private static final String ANONYMOUS_KEY = "anonymous-key";

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

    private AuthDataAccessor authDataAccessor;

    @BeforeEach
    void setUp() throws Exception {
        securityProperties = new SecurityProperties();
        securityProperties.setJwtIssuer("ApacheSyncope");
        securityProperties.setJwsAlgorithm("HS512");
        securityProperties.setJwsKey(SecureRandomUtils.generateRandomLetters(64));
        securityProperties.setAdminUser(ADMIN);
        securityProperties.setAdminPassword("admin-password");
        securityProperties.setAdminPasswordAlgorithm(CipherAlgorithm.SHA512);
        securityProperties.setAnonymousUser(ANONYMOUS);
        securityProperties.setAnonymousKey(ANONYMOUS_KEY);

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        encryptorManager = mock(EncryptorManager.class);
        encryptor = mock(Encryptor.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);
        when(encryptor.verify(any(), any(), any())).thenReturn(true);
        when(encryptor.decode(any(), any())).thenReturn("decoded-secret");

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
        when(confParamOps.get(anyString(), eq(StandardConfParams.LOG_LAST_LOGIN_DATE), any(), eq(boolean.class))).
                thenReturn(true);
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(),
                eq(String[].class))).thenReturn(new String[] { "active" });
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_ATTRIBUTES), any(),
                eq(String[].class))).thenReturn(new String[] { "username" });

        authDataAccessor = newAccessor();
    }

    private AuthDataAccessor newAccessor(final JWTSSOProvider... providers) {
        return new AuthDataAccessor(
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
                List.of(providers));
    }

    private Authentication usernamePassword(final String username, final String credentials) {
        return usernamePassword(username, credentials,
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
    }

    private Authentication usernamePassword(
            final String username,
            final String credentials,
            final SyncopeAuthenticationDetails details) {

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getCredentials()).thenReturn(credentials);
        when(authentication.getDetails()).thenReturn(details);
        return authentication;
    }

    private JWTAuthentication jwt(
            final String subject,
            final String issuer,
            final String jwtID,
            final String delegatedBy) {

        JWTClaimsSet claims = new JWTClaimsSet.Builder().
                issuer(issuer).
                subject(subject).
                jwtID(jwtID).
                issueTime(new Date()).
                build();
        return new JWTAuthentication(
                claims,
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, delegatedBy));
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

    private void mfaEnabled(final boolean enabled) {
        when(confParamOps.get(anyString(), eq(StandardConfParams.MFA_ENABLED), any(), eq(boolean.class))).
                thenReturn(enabled);
    }

    private void authenticationStatuses(final String... statuses) {
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(),
                eq(String[].class))).thenReturn(statuses);
    }

    private void authenticationAttributes(final String... attributes) {
        when(confParamOps.get(anyString(), eq(StandardConfParams.AUTHENTICATION_ATTRIBUTES), any(),
                eq(String[].class))).thenReturn(attributes);
    }

    private static Set<String> entitlements(final Set<SyncopeGrantedAuthority> authorities) {
        return authorities.stream().map(SyncopeGrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static void assertContains(final Set<SyncopeGrantedAuthority> authorities, final String entitlement) {
        assertTrue(entitlements(authorities).contains(entitlement),
                "Expected entitlement " + entitlement + " in " + entitlements(authorities));
    }

    @Test
    void getJWTSSOProviderRejectsNullIssuer() {
        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.getJWTSSOProvider(null));
        assertEquals("A null issuer is not permitted", ex.getMessage());
    }

    @Test
    void getJWTSSOProviderRejectsUnknownIssuer() {
        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.getJWTSSOProvider("unknown"));
        assertEquals("Could not find any registered JWTSSOProvider for issuer unknown", ex.getMessage());
    }

    @Test
    void getJWTSSOProviderReturnsMatchingProvider() {
        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        AuthDataAccessor accessor = newAccessor(provider);

        assertSame(provider, accessor.getJWTSSOProvider("issuer"));
    }

    @Test
    void anonymousAuthenticationSucceedsWithConfiguredKey() {
        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword(ANONYMOUS, ANONYMOUS_KEY));

        assertTrue(result.isSuccess());
        assertTrue(result.passwordVerified());
        assertNull(result.user());
        assertNull(result.otpVerified());
        assertEquals(Set.of(IdRepoEntitlement.ANONYMOUS), entitlements(result.authorities()));
        verify(credentialChecker).checkIsDefaultAnonymousKeyInUse();
    }

    @Test
    void anonymousAuthenticationFailsWithWrongKey() {
        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword(ANONYMOUS, "wrong"));

        assertFalse(result.isSuccess());
        assertFalse(result.passwordVerified());
        assertEquals(Set.of(IdRepoEntitlement.ANONYMOUS), entitlements(result.authorities()));
    }

    @Test
    void adminAuthenticationWithoutMfaGrantsAllEntitlements() {
        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword(ADMIN, "admin-password"));

        assertTrue(result.isSuccess());
        assertNull(result.user());
        assertContains(result.authorities(), IdRepoEntitlement.USER_READ);
        assertContains(result.authorities(), IdRepoEntitlement.GROUP_READ);
        verify(credentialChecker).checkIsDefaultAdminPasswordInUse();
    }

    @Test
    void adminAuthenticationWithMfaButNoSecretRequiresEnrollment() {
        mfaEnabled(true);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword(ADMIN, "admin-password:123456"));

        assertTrue(result.isSuccess());
        assertNull(result.otpVerified());
        assertEquals(Set.of(IdRepoEntitlement.MFA_ENROLL), entitlements(result.authorities()));
    }

    @Test
    void adminAuthenticationWithMfaAndValidOtpGrantsAllEntitlements() {
        mfaEnabled(true);
        securityProperties.setAdminMfaSecret("encoded-secret");
        when(totpCodeVerifier.isValidCode("decoded-secret", "123456")).thenReturn(true);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword(ADMIN, "admin-password:123456"));

        assertTrue(result.isSuccess());
        assertTrue(result.otpVerified());
        assertContains(result.authorities(), IdRepoEntitlement.USER_READ);
    }

    @Test
    void adminAuthenticationWithUndecodableMfaSecretThrowsBadCredentials() throws Exception {
        mfaEnabled(true);
        securityProperties.setAdminMfaSecret("encoded-secret");
        when(encryptor.decode(any(), any())).thenThrow(new IllegalStateException("boom"));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authDataAccessor.authenticate(
                        Optional.empty(), usernamePassword(ADMIN, "admin-password:123456")));
        assertEquals("Could not decode admin MFA secret for Master", ex.getMessage());
    }

    @Test
    void adminAuthenticationOnSpecificDomainUsesDomainCredentials() {
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("other");
        when(domain.getAdminCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);
        when(domain.getAdminPassword()).thenReturn("domain-password");
        when(domain.getAdminMfaSecret()).thenReturn(null);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.of(domain), usernamePassword(ADMIN, "domain-password"));

        assertTrue(result.isSuccess());
        verify(credentialChecker, never()).checkIsDefaultAdminPasswordInUse();
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        doReturn(Optional.empty()).when(userDAO).findByUsername("bob");

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> authDataAccessor.authenticate(Optional.empty(), usernamePassword("bob", "password")));
        assertEquals("bob", ex.getMessage());
    }

    @Test
    void suspendedUserCannotAuthenticate() {
        User user = user("bob");
        when(user.isSuspended()).thenReturn(true);
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");

        DisabledException ex = assertThrows(
                DisabledException.class,
                () -> authDataAccessor.authenticate(Optional.empty(), usernamePassword("bob", "password")));
        assertEquals("User bob is suspended", ex.getMessage());
    }

    @Test
    void userWithDisallowedStatusCannotAuthenticate() {
        User user = user("bob");
        when(user.getStatus()).thenReturn("inactive");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        authenticationStatuses("active");

        DisabledException ex = assertThrows(
                DisabledException.class,
                () -> authDataAccessor.authenticate(Optional.empty(), usernamePassword("bob", "password")));
        assertEquals("User bob not allowed to authenticate", ex.getMessage());
    }

    @Test
    void wrongPasswordIncrementsFailedLogins() {
        User user = user("bob");
        when(user.getFailedLogins()).thenReturn(2);
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(encryptor.verify(any(), any(), any())).thenReturn(false);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "wrong"));

        assertFalse(result.isSuccess());
        assertFalse(result.passwordVerified());
        assertNull(result.otpVerified());
        verify(user).setFailedLogins(3);
        verify(userDAO).save(user);
    }

    @Test
    void validPasswordReturnsRoleAuthoritiesAndResetsState() {
        User user = user("bob");
        when(user.getFailedLogins()).thenReturn(2);
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password"));

        assertTrue(result.isSuccess());
        assertContains(result.authorities(), IdRepoEntitlement.USER_READ);
        verify(user).setLastLoginDate(any());
        verify(user).setFailedLogins(0);
        verify(userDAO).save(user);
    }

    @Test
    void userWithMfaButNoSecretRequiresEnrollment() {
        mfaEnabled(true);
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password:123456"));

        assertTrue(result.isSuccess());
        assertNull(result.otpVerified());
        assertEquals(Set.of(IdRepoEntitlement.MFA_ENROLL), entitlements(result.authorities()));
    }

    @Test
    void userWithMfaAndValidTotpAuthenticates() {
        mfaEnabled(true);
        User user = user("bob");
        when(user.getMfa()).thenReturn(new Mfa("totp-secret", "data-uri", new ArrayList<>()));
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);
        when(totpCodeVerifier.isValidCode("totp-secret", "123456")).thenReturn(true);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password:123456"));

        assertTrue(result.isSuccess());
        assertTrue(result.otpVerified());
        assertContains(result.authorities(), IdRepoEntitlement.USER_READ);
    }

    @Test
    void userWithMfaAndInvalidTotpFails() {
        mfaEnabled(true);
        User user = user("bob");
        when(user.getMfa()).thenReturn(new Mfa("totp-secret", "data-uri", new ArrayList<>()));
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(totpCodeVerifier.isValidCode("totp-secret", "123456")).thenReturn(false);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password:123456"));

        assertFalse(result.isSuccess());
        assertFalse(result.otpVerified());
        verify(user).setFailedLogins(1);
    }

    @Test
    void userWithValidRecoveryCodeConsumesIt() {
        mfaEnabled(true);
        User user = user("bob");
        Mfa mfa = new Mfa("totp-secret", "data-uri", new ArrayList<>(List.of("recovery-code")));
        when(user.getMfa()).thenReturn(mfa);
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password:recovery-code"));

        assertTrue(result.isSuccess());
        assertTrue(result.otpVerified());
        verify(user).setMfa(mfa);
    }

    @Test
    void userWithInvalidRecoveryCodeFails() {
        mfaEnabled(true);
        User user = user("bob");
        when(user.getMfa()).thenReturn(new Mfa("totp-secret", "data-uri", new ArrayList<>(List.of("recovery-code"))));
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password:unknown-code"));

        assertFalse(result.isSuccess());
        assertFalse(result.otpVerified());
        verify(user).setFailedLogins(1);
    }

    @Test
    void userCanBeFoundViaCustomAuthenticationAttribute() {
        authenticationAttributes("email");
        User user = user("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);
        doReturn(List.of(user)).when(anySearchDAO).search(any(SearchCond.class), eq(AnyTypeKind.USER));

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob@example.com", "password"));

        assertTrue(result.isSuccess());
        verify(anySearchDAO).search(any(SearchCond.class), eq(AnyTypeKind.USER));
    }

    @Test
    void searchFailureForCustomAuthenticationAttributeYieldsUsernameNotFound() {
        authenticationAttributes("email");
        doThrow(new IllegalArgumentException("bad schema")).
                when(anySearchDAO).search(any(SearchCond.class), eq(AnyTypeKind.USER));

        assertThrows(
                UsernameNotFoundException.class,
                () -> authDataAccessor.authenticate(
                        Optional.empty(), usernamePassword("bob@example.com", "password")));
    }

    @Test
    void customAuthenticationAttributeMatchingMultipleUsersYieldsUsernameNotFound() {
        authenticationAttributes("email");
        doReturn(List.of(user("bob"), user("alice"))).
                when(anySearchDAO).search(any(SearchCond.class), eq(AnyTypeKind.USER));

        assertThrows(
                UsernameNotFoundException.class,
                () -> authDataAccessor.authenticate(
                        Optional.empty(), usernamePassword("shared@example.com", "password")));
    }

    @Test
    void delegationByUsernameIsResolved() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);
        when(userDAO.findKey("delegator")).thenReturn(Optional.of("delegator-key"));
        when(delegationDAO.findValidFor(eq("delegator-key"), eq("bob-key"), any())).
                thenReturn(Optional.of("delegation-key"));

        SyncopeAuthenticationDetails details =
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator");
        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password", details));

        assertTrue(result.isSuccess());
        assertEquals("delegation-key", result.delegationKey());
    }

    @Test
    void delegationByUuidIsResolvedWithoutUserLookup() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);
        when(userDAO.save(user)).thenReturn(user);
        String delegatingKey = UUID.randomUUID().toString();
        when(delegationDAO.findValidFor(eq(delegatingKey), eq("bob-key"), any())).
                thenReturn(Optional.of("delegation-key"));

        SyncopeAuthenticationDetails details =
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, delegatingKey);
        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password", details));

        assertEquals("delegation-key", result.delegationKey());
        verify(userDAO, never()).findKey(anyString());
    }

    @Test
    void unknownDelegatingUserThrowsSessionAuthenticationException() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(userDAO.findKey("ghost")).thenReturn(Optional.empty());

        SyncopeAuthenticationDetails details =
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "ghost");
        SessionAuthenticationException ex = assertThrows(
                SessionAuthenticationException.class,
                () -> authDataAccessor.authenticate(
                        Optional.empty(), usernamePassword("bob", "password", details)));
        assertEquals("Delegating user ghost cannot be found", ex.getMessage());
    }

    @Test
    void missingValidDelegationThrowsSessionAuthenticationException() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(userDAO.findKey("delegator")).thenReturn(Optional.of("delegator-key"));
        when(delegationDAO.findValidFor(eq("delegator-key"), eq("bob-key"), any())).
                thenReturn(Optional.empty());

        SyncopeAuthenticationDetails details =
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator");
        SessionAuthenticationException ex = assertThrows(
                SessionAuthenticationException.class,
                () -> authDataAccessor.authenticate(
                        Optional.empty(), usernamePassword("bob", "password", details)));
        assertEquals("Delegation by delegator-key was requested but none found", ex.getMessage());
    }

    @Test
    void passthroughResourcesAreIntersectedAcrossResourcesAndRealmAncestors() {
        User user = user("bob");

        ExternalResource first = mock(ExternalResource.class);
        ExternalResource second = mock(ExternalResource.class);
        AccountPolicy firstPolicy = mock(AccountPolicy.class);
        AccountPolicy secondPolicy = mock(AccountPolicy.class);
        when(first.getAccountPolicy()).thenReturn(firstPolicy);
        when(second.getAccountPolicy()).thenReturn(secondPolicy);
        when(userDAO.findAllResources(user)).thenReturn(List.of(first, second));
        when(resourceDAO.findByPolicy(firstPolicy)).thenReturn(List.of(first, second));
        when(resourceDAO.findByPolicy(secondPolicy)).thenReturn(List.of(second));

        Realm realm = mock(Realm.class);
        Realm ancestor = mock(Realm.class);
        AccountPolicy ancestorPolicy = mock(AccountPolicy.class);
        when(user.getRealm()).thenReturn(realm);
        when(ancestor.getAccountPolicy()).thenReturn(ancestorPolicy);
        when(realmSearchDAO.findAncestors(realm)).thenReturn(List.of(ancestor));
        when(resourceDAO.findByPolicy(ancestorPolicy)).thenReturn(List.of(second));

        assertEquals(Set.of(second), authDataAccessor.getPassthroughResources(user));
    }

    @Test
    void authenticationFallsBackToPassthroughResource() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(encryptor.verify(any(), any(), any())).thenReturn(false);

        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource");
        AccountPolicy policy = mock(AccountPolicy.class);
        when(resource.getAccountPolicy()).thenReturn(policy);
        when(userDAO.findAllResources(user)).thenReturn(List.of(resource));
        when(resourceDAO.findByPolicy(policy)).thenReturn(List.of(resource));

        Provision provision = mock(Provision.class);
        when(resource.getProvisionByAnyType(AnyTypeKind.USER.name())).thenReturn(Optional.of(provision));
        when(mappingManager.getConnObjectKeyValue(user, resource, provision)).
                thenReturn(Optional.of("conn-key"));
        Connector connector = mock(Connector.class);
        when(connectorManager.getConnector(resource)).thenReturn(connector);
        when(connector.authenticate("conn-key", "password", null)).thenReturn(new Uid("uid"));

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password"));

        assertTrue(result.passwordVerified());
        assertTrue(result.isSuccess());
        verify(connector).authenticate("conn-key", "password", null);
    }

    @Test
    void passthroughResourceWithoutProvisionLeavesAuthenticationFailed() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        when(userDAO.save(user)).thenReturn(user);
        when(encryptor.verify(any(), any(), any())).thenReturn(false);

        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource");
        AccountPolicy policy = mock(AccountPolicy.class);
        when(resource.getAccountPolicy()).thenReturn(policy);
        when(userDAO.findAllResources(user)).thenReturn(List.of(resource));
        when(resourceDAO.findByPolicy(policy)).thenReturn(List.of(resource));
        when(resource.getProvisionByAnyType(AnyTypeKind.USER.name())).thenReturn(Optional.empty());

        AuthDataAccessor.UsernamePasswordAuthResult result = authDataAccessor.authenticate(
                Optional.empty(), usernamePassword("bob", "password"));

        assertFalse(result.passwordVerified());
        assertFalse(result.isSuccess());
        verify(connectorManager, never()).getConnector(any());
    }

    @Test
    void userWhoMustChangePasswordGetsOnlyThatAuthority() {
        User user = user("bob");
        when(user.isMustChangePassword()).thenReturn(true);

        assertEquals(
                Set.of(IdRepoEntitlement.MUST_CHANGE_PASSWORD),
                entitlements(authDataAccessor.getUserAuthorities(user)));
    }

    @Test
    void managedEntitiesGrantManagerEntitlements() {
        User user = user("bob");

        Realm managedUserRealm = mock(Realm.class);
        when(managedUserRealm.getFullPath()).thenReturn("/users");
        User managedUser = user("managed");
        when(managedUser.getRealm()).thenReturn(managedUserRealm);
        when(userDAO.findManagedUsers("bob-key")).thenReturn(List.of(managedUser));

        Realm managedGroupRealm = mock(Realm.class);
        when(managedGroupRealm.getFullPath()).thenReturn("/groups");
        Group managedGroup = mock(Group.class);
        when(managedGroup.getKey()).thenReturn("group-key");
        when(managedGroup.getRealm()).thenReturn(managedGroupRealm);
        when(userDAO.findManagedGroups("bob-key")).thenReturn(List.of(managedGroup));

        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("PRINTER");
        Realm managedAnyObjectRealm = mock(Realm.class);
        when(managedAnyObjectRealm.getFullPath()).thenReturn("/printers");
        AnyObject managedAnyObject = mock(AnyObject.class);
        when(managedAnyObject.getKey()).thenReturn("any-key");
        when(managedAnyObject.getType()).thenReturn(anyType);
        when(managedAnyObject.getRealm()).thenReturn(managedAnyObjectRealm);
        when(userDAO.findManagedAnyObjects("bob-key")).thenReturn(List.of(managedAnyObject));

        Set<SyncopeGrantedAuthority> authorities = authDataAccessor.getUserAuthorities(user);

        assertContains(authorities, IdRepoEntitlement.USER_READ);
        assertContains(authorities, IdRepoEntitlement.GROUP_READ);
        assertContains(authorities, "PRINTER_READ");
        assertContains(authorities, IdRepoEntitlement.ANYTYPE_READ);
    }

    @Test
    void anonymousUsernameGetsAnonymousAuthorities() {
        assertEquals(
                Set.of(IdRepoEntitlement.ANONYMOUS),
                entitlements(authDataAccessor.getAuthorities(ANONYMOUS, null)));
    }

    @Test
    void adminUsernameGetsAllAuthorities() {
        assertContains(authDataAccessor.getAuthorities(ADMIN, null), IdRepoEntitlement.USER_READ);
    }

    @Test
    void unknownDelegationThrowsUsernameNotFound() {
        doReturn(Optional.empty()).when(delegationDAO).findById("delegation-key");

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> authDataAccessor.getAuthorities("bob", "delegation-key"));
        assertEquals("Could not find delegation delegation-key", ex.getMessage());
    }

    @Test
    void delegationWithoutRolesUsesDelegatingUserAuthorities() {
        User delegating = user("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(delegating);

        Delegation delegation = mock(Delegation.class);
        doReturn(Set.of()).when(delegation).getRoles();
        when(delegation.getDelegating()).thenReturn(delegating);
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("delegation-key");

        assertContains(
                authDataAccessor.getAuthorities("bob", "delegation-key"),
                IdRepoEntitlement.USER_READ);
    }

    @Test
    void delegationWithRolesUsesDelegatedAuthorities() {
        Delegation delegation = mock(Delegation.class);
        doReturn(Set.of(role(IdRepoEntitlement.GROUP_READ, "/groups"))).when(delegation).getRoles();
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("delegation-key");

        assertContains(
                authDataAccessor.getAuthorities("bob", "delegation-key"),
                IdRepoEntitlement.GROUP_READ);
    }

    @Test
    void unknownUsernameForAuthoritiesThrowsUsernameNotFound() {
        doReturn(Optional.empty()).when(userDAO).findByUsername("bob");

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> authDataAccessor.getAuthorities("bob", null));
        assertEquals("Could not find any user with username bob", ex.getMessage());
    }

    @Test
    void existingUsernameGetsUserAuthorities() {
        User user = user("bob");
        doReturn(Optional.of(user)).when(userDAO).findByUsername("bob");
        Role userRole = role(IdRepoEntitlement.USER_READ, "/");
        doReturn(List.of(userRole)).when(userDAO).findAllRoles(user);

        assertContains(authDataAccessor.getAuthorities("bob", null), IdRepoEntitlement.USER_READ);
    }

    @Test
    void jwtAdminWithoutAccessTokenThrows() {
        JWTAuthentication authentication = jwt(ADMIN, securityProperties.getJwtIssuer(), "jti", null);
        doReturn(Optional.empty()).when(accessTokenDAO).findById("jti");

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.authenticate(authentication));
        assertEquals("Could not find an Access Token for JWT jti", ex.getMessage());
    }

    @Test
    void jwtAdminWithMatchingAccessTokenGetsAllAuthorities() {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getOwner()).thenReturn(ADMIN);
        doReturn(Optional.of(accessToken)).when(accessTokenDAO).findById("jti");

        AuthDataAccessor.JWTAuthResult result = authDataAccessor.authenticate(
                jwt(ADMIN, securityProperties.getJwtIssuer(), "jti", null));

        assertEquals(ADMIN, result.username());
        assertContains(result.authorities(), IdRepoEntitlement.USER_READ);
    }

    @Test
    void jwtWithUnknownIssuerThrows() {
        JWTAuthentication authentication = jwt("bob", "unknown", "jti", null);

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.authenticate(authentication));
    }

    @Test
    void jwtNotResolvableToUserThrows() {
        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        when(provider.resolve(any())).thenReturn(Optional.empty());
        AuthDataAccessor accessor = newAccessor(provider);

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> accessor.authenticate(jwt("bob", "issuer", "jti", null)));
        assertEquals("Could not find User bob for JWT jti", ex.getMessage());
    }

    @Test
    void jwtForSuspendedUserIsRejected() {
        User user = user("bob");
        when(user.isSuspended()).thenReturn(true);
        JWTSSOProvider provider = providerResolving(user, Set.of());

        DisabledException ex = assertThrows(
                DisabledException.class,
                () -> newAccessor(provider).authenticate(jwt("bob", "issuer", "jti", null)));
        assertEquals("User bob is suspended", ex.getMessage());
    }

    @Test
    void jwtForUserWithDisallowedStatusIsRejected() {
        User user = user("bob");
        when(user.getStatus()).thenReturn("inactive");
        authenticationStatuses("active");
        JWTSSOProvider provider = providerResolving(user, Set.of());

        DisabledException ex = assertThrows(
                DisabledException.class,
                () -> newAccessor(provider).authenticate(jwt("bob", "issuer", "jti", null)));
        assertEquals("User bob not allowed to authenticate", ex.getMessage());
    }

    @Test
    void jwtForUserWhoMustChangePasswordResetsAuthorities() {
        User user = user("bob");
        when(user.isMustChangePassword()).thenReturn(true);
        JWTSSOProvider provider = providerResolving(
                user, Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ)));

        AuthDataAccessor.JWTAuthResult result = newAccessor(provider).
                authenticate(jwt("bob", "issuer", "jti", null));

        assertEquals(Set.of(IdRepoEntitlement.MUST_CHANGE_PASSWORD), entitlements(result.authorities()));
    }

    @Test
    void jwtWithNullResolvedAuthoritiesYieldsEmptyAuthorities() {
        JWTSSOProvider provider = providerResolving(user("bob"), null);

        AuthDataAccessor.JWTAuthResult result = newAccessor(provider).
                authenticate(jwt("bob", "issuer", "jti", null));

        assertTrue(result.authorities().isEmpty());
    }

    @Test
    void jwtWithResolvedAuthoritiesPreservesThem() {
        Set<SyncopeGrantedAuthority> resolved =
                Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ));
        JWTSSOProvider provider = providerResolving(user("bob"), resolved);

        AuthDataAccessor.JWTAuthResult result = newAccessor(provider).
                authenticate(jwt("bob", "issuer", "jti", null));

        assertEquals(Set.of(IdRepoEntitlement.USER_READ), entitlements(result.authorities()));
    }

    @Test
    void jwtWithDelegationUsesDelegatedAuthorities() {
        User user = user("bob");
        JWTSSOProvider provider = providerResolving(
                user, Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ)));

        when(userDAO.findKey("delegator")).thenReturn(Optional.of("delegator-key"));
        when(delegationDAO.findValidFor(eq("delegator-key"), eq("bob-key"), any())).
                thenReturn(Optional.of("delegation-key"));
        Delegation delegation = mock(Delegation.class);
        doReturn(Set.of(role(IdRepoEntitlement.GROUP_READ, "/"))).when(delegation).getRoles();
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("delegation-key");

        AuthDataAccessor.JWTAuthResult result = newAccessor(provider).
                authenticate(jwt("bob", "issuer", "jti", "delegator"));

        assertEquals("bob", result.username());
        assertEquals(Set.of(IdRepoEntitlement.GROUP_READ), entitlements(result.authorities()));
    }

    private JWTSSOProvider providerResolving(
            final User user, final Set<SyncopeGrantedAuthority> authorities) {

        JWTSSOProvider provider = mock(JWTSSOProvider.class);
        when(provider.getIssuer()).thenReturn("issuer");
        when(provider.resolve(any())).
                thenReturn(Optional.of(new JWTSSOProvider.ResolvedClaims(user, authorities)));
        return provider;
    }

    @Test
    void lowPrivilegeTokenIdCanBeReboundToAdminWithDefaultSigningKey() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().
                issuer(securityProperties.getJwtIssuer()).
                subject(securityProperties.getAdminUser()).
                jwtID(LOW_PRIVILEGE_JTI).
                issueTime(new Date()).
                notBeforeTime(new Date()).
                expirationTime(Date.from(OffsetDateTime.now().plusMinutes(10).toInstant())).
                build();
        SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        forged.sign(new AccessTokenJWSSigner(JWSAlgorithm.HS512, securityProperties.getJwsKey()));

        assertTrue(forged.verify(new AccessTokenJWSVerifier(JWSAlgorithm.HS512, securityProperties.getJwsKey())));

        JWTAuthentication authentication = new JWTAuthentication(
                forged.getJWTClaimsSet(),
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));

        AccessToken lowPrivilegeToken = mock(AccessToken.class);
        when(lowPrivilegeToken.getOwner()).thenReturn(LOW_PRIVILEGE_USER);
        doReturn(Optional.of(lowPrivilegeToken)).when(accessTokenDAO).findById(LOW_PRIVILEGE_JTI);

        AuthenticationCredentialsNotFoundException ex = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> authDataAccessor.authenticate(authentication));
        assertEquals("Access Token owner does not match JWT subject", ex.getMessage());
    }

    @Test
    void removeExpiredDeletesAccessToken() {
        authDataAccessor.removeExpired("token-key");

        verify(accessTokenDAO).deleteById("token-key");
    }

    @Test
    void auditAppendsDelegationKeyToUsername() {
        authDataAccessor.audit(
                SyncopeConstants.MASTER_DOMAIN,
                "bob",
                "delegation-key",
                OpEvent.Outcome.SUCCESS,
                "output",
                "input");

        verify(auditManager).audit(
                eq(SyncopeConstants.MASTER_DOMAIN),
                eq("bob [under delegation delegation-key]"),
                eq(OpEvent.CategoryType.LOGIC),
                eq(OpEvent.AUTHENTICATION_CATEGORY),
                isNull(),
                eq(OpEvent.LOGIN_OP),
                eq(OpEvent.Outcome.SUCCESS),
                isNull(),
                eq("output"),
                eq((Object) "input"));
    }

    @Test
    void auditWithoutDelegationKeepsUsername() {
        authDataAccessor.audit(
                SyncopeConstants.MASTER_DOMAIN,
                "bob",
                null,
                OpEvent.Outcome.FAILURE,
                "output");

        verify(auditManager).audit(
                eq(SyncopeConstants.MASTER_DOMAIN),
                eq("bob"),
                eq(OpEvent.CategoryType.LOGIC),
                eq(OpEvent.AUTHENTICATION_CATEGORY),
                isNull(),
                eq(OpEvent.LOGIN_OP),
                eq(OpEvent.Outcome.FAILURE),
                isNull(),
                eq("output"));
    }
}
