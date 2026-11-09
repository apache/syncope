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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class AccessTokenLogicTest {

    private SecurityProperties securityProperties;

    private EncryptorManager encryptorManager;

    private AccessTokenDataBinder binder;

    private AccessTokenDAO accessTokenDAO;

    private AccessTokenLogic logic;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setAnonymousUser("anonymous");

        encryptorManager = mock(EncryptorManager.class);
        binder = mock(AccessTokenDataBinder.class);
        accessTokenDAO = mock(AccessTokenDAO.class);

        logic = new AccessTokenLogic(securityProperties, encryptorManager, binder, accessTokenDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void loginAsAnonymous() {
        LogicTestAuth.authenticate("anonymous");

        assertThrows(SyncopeClientException.class, () -> logic.login());
    }

    @Test
    void login() {
        LogicTestAuth.authenticate("user");

        AccessTokenDataBinder.AccessTokenInfo info =
                new AccessTokenDataBinder.AccessTokenInfo("jwt", OffsetDateTime.now());
        when(binder.create(any(), eq("user"), anyMap(), any(), anyBoolean())).thenReturn(info);

        assertSame(info, logic.login());
    }

    @Test
    void refreshMissing() {
        LogicTestAuth.authenticate("user");
        when(accessTokenDAO.findByOwner("user")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.refresh());
    }

    @Test
    void refresh() {
        LogicTestAuth.authenticate("user");
        AccessToken accessToken = mock(AccessToken.class);
        AccessTokenDataBinder.AccessTokenInfo info =
                new AccessTokenDataBinder.AccessTokenInfo("jwt", OffsetDateTime.now());

        when(accessTokenDAO.findByOwner("user")).thenReturn(Optional.of(accessToken));
        when(binder.update(eq(accessToken), any())).thenReturn(info);

        assertSame(info, logic.refresh());
    }

    @Test
    void logoutMissing() {
        LogicTestAuth.authenticate("user");
        when(accessTokenDAO.findByOwner("user")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.logout());
    }

    @Test
    void logout() {
        LogicTestAuth.authenticate("user");
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getKey()).thenReturn("key");
        when(accessTokenDAO.findByOwner("user")).thenReturn(Optional.of(accessToken));

        logic.logout();

        verify(accessTokenDAO).deleteById("key");
    }

    @Test
    void list() {
        Pageable pageable = Pageable.unpaged();
        when(accessTokenDAO.findAll(pageable)).thenReturn(Page.empty(pageable));

        assertEquals(0, logic.list(pageable).getTotalElements());
    }

    @Test
    void listMapped() {
        Pageable pageable = Pageable.unpaged();
        AccessToken accessToken = mock(AccessToken.class);
        AccessTokenTO to = new AccessTokenTO();
        to.setKey("key");

        doReturn(new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(accessToken), pageable, 1)).when(accessTokenDAO).findAll(pageable);
        when(binder.getAccessTokenTO(accessToken)).thenReturn(to);

        assertEquals(1, logic.list(pageable).getTotalElements());
        assertSame(to, logic.list(pageable).getContent().getFirst());
    }

    @Test
    void delete() {
        logic.delete("key");

        verify(accessTokenDAO).deleteById("key");
    }

    @Test
    void resolveReference() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(
                AccessTokenLogic.class.getMethod("delete", String.class), "key"));
    }
}
