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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.spring.security.throttle.ThrottlerAttempts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.InvalidOneTimeTokenException;
import org.springframework.security.core.Authentication;

class UsernamePasswordAuthenticationProviderTest {

    private final DomainOps domainOps = mock(DomainOps.class);

    private final AuthDataAccessor dataAccessor = mock(AuthDataAccessor.class);

    private final UserProvisioningManager provisioningManager = mock(UserProvisioningManager.class);

    private final EncryptorManager encryptorManager = mock(EncryptorManager.class);

    @SuppressWarnings("unchecked")
    private final Cache<String, ThrottlerAttempts> throttlerCache = mock(Cache.class);

    private SecurityProperties securityProperties;

    private StaticApplicationContext context;

    private UsernamePasswordAuthenticationProvider provider;

    private ConfigurableApplicationContext previousContext;

    private DefaultListableBeanFactory previousBeanFactory;

    @BeforeEach
    void setUp() {
        previousContext = ApplicationContextProvider.getApplicationContext();
        previousBeanFactory = ApplicationContextProvider.getBeanFactory();

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");
        securityProperties.getAuthenticationThrottle().setEnabled(false);

        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.refresh();

        ApplicationContextProvider.setApplicationContext(context);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) context.getBeanFactory());

        provider = new UsernamePasswordAuthenticationProvider(
                domainOps,
                dataAccessor,
                provisioningManager,
                securityProperties,
                encryptorManager,
                throttlerCache);
    }

    @AfterEach
    void tearDown() {
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
        context.close();
    }

    private UsernamePasswordAuthenticationToken authentication(final String domain) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("bob", "pwd");
        authentication.setDetails(new SyncopeAuthenticationDetails(domain, null));
        return authentication;
    }

    private User user() {
        User user = mock(User.class);
        when(user.getKey()).thenReturn("user-key");
        when(user.getUsername()).thenReturn("bob");
        return user;
    }

    private AuthDataAccessor.UsernamePasswordAuthResult result(
            final User user,
            final boolean passwordVerified,
            final Boolean otpVerified,
            final Set<SyncopeGrantedAuthority> authorities) {

        return new AuthDataAccessor.UsernamePasswordAuthResult(
                user, passwordVerified, otpVerified, null, authorities);
    }

    @Test
    void supportsUsernamePasswordAuthentication() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(JWTAuthentication.class));
    }

    @Test
    void successfulAuthenticationOnMasterDomain() {
        User user = user();
        Set<SyncopeGrantedAuthority> authorities =
                Set.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ));
        UsernamePasswordAuthenticationToken authentication = authentication(SyncopeConstants.MASTER_DOMAIN);

        when(dataAccessor.authenticate(eq(Optional.empty()), any())).
                thenReturn(result(user, true, true, authorities));

        Authentication result = provider.authenticate(authentication);

        assertEquals("bob", result.getName());
        assertEquals(authorities, Set.copyOf(result.getAuthorities()));
        assertSame(authentication.getDetails(), result.getDetails());
        assertNull(result.getCredentials());
        verify(domainOps, never()).read(anyString());
        verify(dataAccessor).audit(
                eq(SyncopeConstants.MASTER_DOMAIN),
                eq("bob"),
                isNull(),
                eq(OpEvent.Outcome.SUCCESS),
                eq(true),
                eq(authentication),
                anyString());
    }

    @Test
    void successfulAuthenticationOnConfiguredDomain() {
        User user = user();
        Domain domain = mock(Domain.class);
        UsernamePasswordAuthenticationToken authentication = authentication("other");

        when(domainOps.read("other")).thenReturn(domain);
        when(dataAccessor.authenticate(eq(Optional.of(domain)), any())).
                thenReturn(result(user, true, true, Set.of()));

        Authentication result = provider.authenticate(authentication);

        assertEquals("bob", result.getName());
        verify(domainOps).read("other");
    }

    @Test
    void unknownDomainIsRejected() {
        when(domainOps.read("other")).thenThrow(new NotFoundException("not found"));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(authentication("other")));

        assertEquals("Could not find domain other", ex.getMessage());
        assertInstanceOf(NotFoundException.class, ex.getCause());
        verify(dataAccessor, never()).authenticate(any(), any());
    }

    @Test
    void keymasterFailureIsRejected() {
        when(domainOps.read("other")).thenThrow(new KeymasterException("boom"));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(authentication("other")));

        assertEquals("Could not find domain other", ex.getMessage());
        assertInstanceOf(KeymasterException.class, ex.getCause());
    }

    @Test
    void failedPasswordSuspendsUserAndIsRejected() {
        User user = user();
        UsernamePasswordAuthenticationToken authentication = authentication(SyncopeConstants.MASTER_DOMAIN);

        when(dataAccessor.authenticate(eq(Optional.empty()), any())).
                thenReturn(result(user, false, null, Set.of()));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(authentication));

        assertEquals("bob: invalid password provided", ex.getMessage());
        verify(provisioningManager).suspendOnAuthFailures("user-key", "admin", "Failed authentication");
        verify(dataAccessor).audit(
                eq(SyncopeConstants.MASTER_DOMAIN),
                eq("bob"),
                isNull(),
                eq(OpEvent.Outcome.FAILURE),
                eq(false),
                eq(authentication),
                eq("Not authenticated"));
    }

    @Test
    void failedOtpIsRejected() {
        User user = user();
        UsernamePasswordAuthenticationToken authentication = authentication(SyncopeConstants.MASTER_DOMAIN);

        when(dataAccessor.authenticate(eq(Optional.empty()), any())).
                thenReturn(result(user, true, false, Set.of()));

        InvalidOneTimeTokenException ex = assertThrows(
                InvalidOneTimeTokenException.class,
                () -> provider.authenticate(authentication));

        assertEquals("bob: invalid OTP or recovery code provided", ex.getMessage());
        verify(provisioningManager).suspendOnAuthFailures("user-key", "admin", "Failed authentication");
    }

    @Test
    void missingUserFallsBackToAuthenticatingPrincipal() {
        UsernamePasswordAuthenticationToken authentication = authentication(SyncopeConstants.MASTER_DOMAIN);

        when(dataAccessor.authenticate(eq(Optional.empty()), any())).
                thenReturn(result(null, true, true, Set.of()));

        Authentication result = provider.authenticate(authentication);

        assertEquals("bob", result.getName());
        verify(provisioningManager, never()).suspendOnAuthFailures(any(), any(), any());
    }

    @Test
    void auditUsesDelegationKeyOnlyWhenProvided() {
        User user = user();
        UsernamePasswordAuthenticationToken authentication = authentication(SyncopeConstants.MASTER_DOMAIN);
        AuthDataAccessor.UsernamePasswordAuthResult authResult = new AuthDataAccessor.UsernamePasswordAuthResult(
                user, true, true, "delegation-key", Set.of());

        when(dataAccessor.authenticate(eq(Optional.empty()), any())).thenReturn(authResult);

        provider.authenticate(authentication);

        verify(dataAccessor).audit(
                eq(SyncopeConstants.MASTER_DOMAIN),
                eq("bob"),
                eq("delegation-key"),
                eq(OpEvent.Outcome.SUCCESS),
                eq(true),
                eq(authentication),
                anyString());
    }
}
