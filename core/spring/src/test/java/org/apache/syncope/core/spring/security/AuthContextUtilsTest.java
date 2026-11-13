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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthContextUtilsTest {

    private StaticApplicationContext context;

    private SecurityProperties securityProperties;

    private UserDAO userDAO;

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

        userDAO = mock(UserDAO.class);

        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.getBeanFactory().registerSingleton("userDAO", userDAO);
        context.refresh();

        ApplicationContextProvider.setApplicationContext(context);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) context.getBeanFactory());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
        context.close();
    }

    private Authentication authentication(final String username, final SyncopeAuthenticationDetails details) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                "pwd",
                List.of(
                        new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a"),
                        new SyncopeGrantedAuthority(IdRepoEntitlement.GROUP_READ, "/b")));
        authentication.setDetails(details);
        return authentication;
    }

    @Test
    void getUsernameWithoutAuthentication() {
        assertEquals("unauthenticated", AuthContextUtils.getUsername());
    }

    @Test
    void getUsernameWithAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null)));

        assertEquals("bob", AuthContextUtils.getUsername());
    }

    @Test
    void updateUsernameKeepsCredentialsAuthoritiesAndDetails() {
        SyncopeAuthenticationDetails details =
                new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator");
        Authentication original = authentication("bob", details);
        SecurityContextHolder.getContext().setAuthentication(original);

        AuthContextUtils.updateUsername("alice");

        Authentication updated = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("alice", updated.getName());
        assertEquals("pwd", updated.getCredentials());
        assertSame(details, updated.getDetails());
        assertEquals(original.getAuthorities(), updated.getAuthorities());
    }

    @Test
    void getDelegatedBy() {
        assertTrue(AuthContextUtils.getDelegatedBy().isEmpty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bob", null, List.of()));
        assertTrue(AuthContextUtils.getDelegatedBy().isEmpty());

        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator")));
        assertEquals("delegator", AuthContextUtils.getDelegatedBy().orElseThrow());
    }

    @Test
    void getDomain() {
        assertEquals(SyncopeConstants.MASTER_DOMAIN, AuthContextUtils.getDomain());

        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails(" ", null)));
        assertEquals(SyncopeConstants.MASTER_DOMAIN, AuthContextUtils.getDomain());

        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails("other", null)));
        assertEquals("other", AuthContextUtils.getDomain());
    }

    @Test
    void getAuthoritiesAndAuthorizations() {
        assertTrue(AuthContextUtils.getAuthorities().isEmpty());
        assertTrue(AuthContextUtils.getAuthorizations().isEmpty());

        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null)));

        assertEquals(
                Set.of(IdRepoEntitlement.USER_READ, IdRepoEntitlement.GROUP_READ),
                AuthContextUtils.getAuthorities().stream().map(SyncopeGrantedAuthority::getAuthority).
                        collect(java.util.stream.Collectors.toSet()));

        Map<String, Set<String>> authorizations = AuthContextUtils.getAuthorizations();
        assertEquals(Set.of("/a"), authorizations.get(IdRepoEntitlement.USER_READ));
        assertEquals(Set.of("/b"), authorizations.get(IdRepoEntitlement.GROUP_READ));
    }

    @Test
    void getWhoWithoutDelegation() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bob", null, List.of()));

        assertEquals("bob", AuthContextUtils.getWho());
    }

    @Test
    void getWhoWithDelegation() {
        when(userDAO.findUsername("delegator")).thenReturn(java.util.Optional.of("Delegator Name"));
        SecurityContextHolder.getContext().setAuthentication(
                authentication("bob", new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, "delegator")));

        assertEquals("bob [delegated by Delegator Name]", AuthContextUtils.getWho());
    }

    @Test
    void callAsSetsAndRestoresAuthentication() throws Exception {
        Authentication original = authentication("bob", null);
        SecurityContextHolder.getContext().setAuthentication(original);

        String username = AuthContextUtils.callAs(
                "other",
                "alice",
                List.of(new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ)),
                () -> {
                    assertEquals("alice", AuthContextUtils.getUsername());
                    assertEquals("other", AuthContextUtils.getDomain());
                    return AuthContextUtils.getUsername();
                });

        assertEquals("alice", username);
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void callAsPropagatesRuntimeExceptionAndRestoresAuthentication() {
        Authentication original = authentication("bob", null);
        SecurityContextHolder.getContext().setAuthentication(original);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> AuthContextUtils.callAs(
                        SyncopeConstants.MASTER_DOMAIN,
                        "alice",
                        List.of(),
                        () -> {
                            throw new IllegalStateException("boom");
                        }));

        assertEquals("boom", ex.getMessage());
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void callAsWrapsCheckedExceptionAndRestoresAuthentication() {
        Authentication original = authentication("bob", null);
        SecurityContextHolder.getContext().setAuthentication(original);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> AuthContextUtils.callAs(
                        SyncopeConstants.MASTER_DOMAIN,
                        "alice",
                        List.of(),
                        () -> {
                            throw new Exception("boom");
                        }));

        assertTrue(ex.getCause() instanceof Exception);
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void callAsAdminUsesConfiguredAdminUser() {
        String username = AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN,
                AuthContextUtils::getUsername);

        assertEquals("admin", username);
    }

    @Test
    void runAsAdminExecutesRunnable() {
        boolean[] executed = { false };

        AuthContextUtils.runAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> executed[0] = true);

        assertTrue(executed[0]);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertFalse(AuthContextUtils.getAuthorities().contains(
                new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ)));
    }
}
