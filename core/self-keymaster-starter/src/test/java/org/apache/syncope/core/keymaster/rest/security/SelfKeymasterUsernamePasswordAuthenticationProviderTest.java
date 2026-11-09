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
package org.apache.syncope.core.keymaster.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import javax.cache.Cache;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.apache.syncope.core.spring.security.throttle.ThrottlerAttempts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class SelfKeymasterUsernamePasswordAuthenticationProviderTest {

    private final DomainOps domainOps = mock(DomainOps.class);

    private final AuthDataAccessor dataAccessor = mock(AuthDataAccessor.class);

    private final UserProvisioningManager provisioningManager = mock(UserProvisioningManager.class);

    private final EncryptorManager encryptorManager = mock(EncryptorManager.class);

    @SuppressWarnings("unchecked")
    private final Cache<String, ThrottlerAttempts> throttlerCache = mock(Cache.class);

    private SecurityProperties securityProperties;

    private KeymasterProperties keymasterProperties;

    private StaticApplicationContext context;

    private SelfKeymasterUsernamePasswordAuthenticationProvider provider;

    private ConfigurableApplicationContext previousContext;

    private DefaultListableBeanFactory previousBeanFactory;

    @BeforeEach
    void setUp() {
        previousContext = ApplicationContextProvider.getApplicationContext();
        previousBeanFactory = ApplicationContextProvider.getBeanFactory();

        SecurityContextHolder.clearContext();

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");
        securityProperties.getAuthenticationThrottle().setEnabled(false);

        keymasterProperties = new KeymasterProperties();
        keymasterProperties.setUsername("keymaster");
        keymasterProperties.setPassword("keymaster-password");

        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.refresh();

        ApplicationContextProvider.setApplicationContext(context);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) context.getBeanFactory());

        provider = new SelfKeymasterUsernamePasswordAuthenticationProvider(
                domainOps,
                dataAccessor,
                provisioningManager,
                securityProperties,
                encryptorManager,
                throttlerCache,
                keymasterProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
        context.close();
    }

    @Test
    void supportsUsernamePasswordAuthentication() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void successfulAuthenticationAsKeymasterUser() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);
        Set<SyncopeGrantedAuthority> authorities = Set.of(authority);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("keymaster", "keymaster-password");
        authentication.setDetails(new SyncopeAuthenticationDetails(
                SyncopeConstants.MASTER_DOMAIN, null));

        when(dataAccessor.getAuthorities(eq("keymaster"), isNull())).
                thenReturn(authorities);

        Authentication result = provider.authenticate(authentication);

        assertEquals("keymaster", result.getName());
        assertNull(result.getCredentials());
        assertSame(authentication.getDetails(), result.getDetails());
        verify(dataAccessor, never()).authenticate(any(), any());
    }

    @Test
    void failedAuthenticationAsKeymasterUserWithWrongPassword() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("keymaster", "wrong-password");
        authentication.setDetails(new SyncopeAuthenticationDetails(
                SyncopeConstants.MASTER_DOMAIN, null));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(authentication));

        assertEquals("keymaster: invalid password provided", ex.getMessage());
        verify(dataAccessor, never()).authenticate(any(), any());
    }

    @Test
    void nonKeymasterUserDelegatesToSuper() {
        User user = mock(User.class);
        when(user.getKey()).thenReturn("user-key");
        when(user.getUsername()).thenReturn("bob");
        Set<SyncopeGrantedAuthority> authorities = Set.of();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("bob", "pwd");
        authentication.setDetails(new SyncopeAuthenticationDetails("other", null));

        when(domainOps.read("other")).thenThrow(new NotFoundException("not found"));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(authentication));

        assertEquals("Could not find domain other", ex.getMessage());
        assertInstanceOf(NotFoundException.class, ex.getCause());
        verify(domainOps).read("other");
    }
}
