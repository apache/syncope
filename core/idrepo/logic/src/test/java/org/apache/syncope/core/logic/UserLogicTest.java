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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class UserLogicTest {

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private UserDAO userDAO;

    private UserDataBinder binder;

    private UserProvisioningManager provisioningManager;

    private ConfParamOps confParamOps;

    private AnySearchDAO searchDAO;

    private UserLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        userDAO = mock(UserDAO.class);
        binder = mock(UserDataBinder.class);
        provisioningManager = mock(UserProvisioningManager.class);
        confParamOps = mock(ConfParamOps.class);
        searchDAO = mock(AnySearchDAO.class);

        logic = new UserLogic(
                realmSearchDAO,
                anyTypeDAO,
                mock(TemplateUtils.class),
                userDAO,
                binder,
                provisioningManager,
                mock(EncryptorManager.class),
                confParamOps,
                mock(GroupDAO.class),
                searchDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void read() {
        UserTO to = new UserTO();
        to.setKey("key");
        when(binder.getUserTO("key")).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void searchMissingRealm() {
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class,
                () -> logic.search(null, Pageable.unpaged(), "/missing", true, false));
    }

    @Test
    void search() {
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(searchDAO.getAllMatchingCond()).thenReturn(mock(SearchCond.class));
        when(searchDAO.count(eq(realm), eq(true), any(), any(SearchCond.class), any())).
                thenReturn(0L);
        when(searchDAO.search(
                eq(realm), eq(true), any(), any(SearchCond.class), any(Pageable.class), any())).
                thenReturn(List.of());

        assertEquals(0, logic.search(null, Pageable.unpaged(),
                SyncopeConstants.ROOT_REALM, true, false).getTotalElements());
    }

    @Test
    void create() {
        UserCR createReq = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();
        Realm realm = mock(Realm.class);
        AnyType anyType = mock(AnyType.class);
        UserTO to = new UserTO();
        to.setKey("key");

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anyTypeDAO.getUser()).thenReturn(anyType);
        when(provisioningManager.create(createReq, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("key", List.of()));
        when(binder.getUserTO("key")).thenReturn(to);

        assertSame(to, logic.create(createReq, false).getEntity());
    }

    @Test
    void update() {
        UserUR updateReq = new UserUR.Builder("key").build();
        Realm realm = mock(Realm.class);
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        UserTO updated = new UserTO();
        updated.setKey("key");

        when(binder.getUserTO("key")).thenReturn(userTO, updated);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.update(updateReq, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(updateReq, List.of()));

        assertSame(updated, logic.update(updateReq, false).getEntity());
    }

    @Test
    void delete() {
        Realm realm = mock(Realm.class);
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);

        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(userDAO.isManager("key")).thenReturn(false);
        when(provisioningManager.delete("key", false, "unauthenticated", "REST")).thenReturn(List.of());
        when(userDAO.existsById("key")).thenReturn(true);

        assertSame(userTO, logic.delete("key", false).getEntity());
    }

    @Test
    void deleteManager() {
        Realm realm = mock(Realm.class);
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);

        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(userDAO.isManager("key")).thenReturn(true);

        assertThrows(SyncopeClientException.class, () -> logic.delete("key", false));
    }

    @Test
    void unlink() {
        Realm realm = mock(Realm.class);
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        UserTO unlinked = new UserTO();
        unlinked.setKey("key");

        when(binder.getUserTO("key")).thenReturn(userTO, unlinked);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.unlink(any(UserUR.class), eq("unauthenticated"), eq("REST"))).thenReturn("key");

        assertSame(unlinked, logic.unlink("key", List.of("resource")));
    }

    @Test
    void verifySecurityAnswerForbidden() {
        when(confParamOps.get(
                "Master", StandardConfParams.PASSWORD_RESET_ALLOWED, false, boolean.class)).thenReturn(false);

        assertThrows(SyncopeClientException.class, () -> logic.verifySecurityAnswer("user", "answer"));
    }

    @Test
    void verifySecurityAnswerForMissingUser() {
        when(confParamOps.get(
                "Master", StandardConfParams.PASSWORD_RESET_ALLOWED, false, boolean.class)).thenReturn(true);
        doReturn(Optional.empty()).when(userDAO).findByUsername("missing");

        assertThrows(NotFoundException.class, () -> logic.verifySecurityAnswer("missing", "answer"));
    }
}
