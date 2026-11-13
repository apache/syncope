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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractUserLogicTest {

    private static class TestLogic extends AbstractUserLogic {

        TestLogic(
                final RealmSearchDAO realmSearchDAO,
                final AnyTypeDAO anyTypeDAO,
                final TemplateUtils templateUtils,
                final UserDAO userDAO,
                final UserDataBinder binder,
                final UserProvisioningManager provisioningManager,
                final EncryptorManager encryptorManager,
                final ConfParamOps confParamOps) {

            super(realmSearchDAO, anyTypeDAO, templateUtils, userDAO, binder, provisioningManager,
                    encryptorManager, confParamOps);
        }

        ProvisioningResult<UserTO> create(final UserCR userCR) {
            return doCreate(userCR, false, false);
        }

        ProvisioningResult<UserTO> update(final UserUR userUR) {
            return doUpdate(userUR, false, false);
        }

        ProvisioningResult<UserTO> status(final StatusR statusR) {
            return doStatus(statusR, false);
        }

        ProvisioningResult<UserTO> delete(final UserTO userTO) {
            return doDelete(userTO, false, false);
        }

        public UserTO requestPasswordReset(final String username) {
            return null;
        }

        public UserTO confirmPasswordReset(final String token) {
            return null;
        }

        public UserTO otherOperation(final Object value) {
            return null;
        }
    }

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private UserDAO userDAO;

    private UserDataBinder binder;

    private UserProvisioningManager provisioningManager;

    private TestLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        userDAO = mock(UserDAO.class);
        binder = mock(UserDataBinder.class);
        provisioningManager = mock(UserProvisioningManager.class);

        logic = new TestLogic(
                realmSearchDAO,
                anyTypeDAO,
                mock(TemplateUtils.class),
                userDAO,
                binder,
                provisioningManager,
                mock(EncryptorManager.class),
                mock(ConfParamOps.class));
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void createMissingRealm() {
        UserCR userCR = new UserCR.Builder("/missing", "user").build();
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(SyncopeClientException.class, () -> logic.create(userCR));
    }

    @Test
    void create() {
        UserCR userCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();
        Realm realm = mock(Realm.class);
        AnyType anyType = mock(AnyType.class);
        UserTO userTO = new UserTO();
        userTO.setKey("key");

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anyTypeDAO.getUser()).thenReturn(anyType);
        doReturn(Optional.empty()).when(realm).getTemplate(anyType);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.create(userCR, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("key", List.of()));
        when(binder.getUserTO("key")).thenReturn(userTO);

        assertSame(userTO, logic.create(userCR).getEntity());
    }

    @Test
    void update() {
        UserUR userUR = new UserUR.Builder("key").build();
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.update(userUR, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));

        assertSame(userTO, logic.update(userUR).getEntity());
    }

    @Test
    void deleteManager() {
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(userDAO.isManager("key")).thenReturn(true);

        assertThrows(SyncopeClientException.class, () -> logic.delete(userTO));
    }

    @Test
    void delete() {
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(userDAO.isManager("key")).thenReturn(false);
        when(provisioningManager.delete("key", false, "unauthenticated", "REST")).thenReturn(List.of());
        when(userDAO.existsById("key")).thenReturn(false);

        assertEquals("key", logic.delete(userTO).getEntity().getKey());
    }

    @Test
    void statusSuspend() {
        assertStatus(StatusRType.SUSPEND);
    }

    @Test
    void statusReactivate() {
        assertStatus(StatusRType.REACTIVATE);
    }

    @Test
    void statusActivate() {
        assertStatus(StatusRType.ACTIVATE);
    }

    private void assertStatus(final StatusRType type) {
        StatusR statusR = new StatusR.Builder("key", type).build();
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        ProvisioningManager.ProvisioningResult<String> result =
                new ProvisioningManager.ProvisioningResult<>("key", List.of());

        switch (type) {
            case SUSPEND ->
                when(provisioningManager.suspend(statusR, false, "unauthenticated", "REST")).thenReturn(result);

            case REACTIVATE ->
                when(provisioningManager.reactivate(statusR, false, "unauthenticated", "REST")).thenReturn(result);

            case ACTIVATE ->
                when(provisioningManager.activate(statusR, false, "unauthenticated", "REST")).thenReturn(result);

            default -> {
            }
        }
        when(binder.getUserTO("key")).thenReturn(userTO);

        assertSame(userTO, logic.status(statusR).getEntity());
    }

    @Test
    void resolvePasswordReset() throws Exception {
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        doReturn(Optional.of("key")).when(userDAO).findKey("user");
        when(binder.getUserTO("key")).thenReturn(userTO);

        Method method = TestLogic.class.getMethod("requestPasswordReset", String.class);

        assertSame(userTO, logic.resolveReference(method, "user"));
    }

    @Test
    void resolveByUuid() throws Exception {
        String uuid = "11111111-1111-1111-1111-111111111111";
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        when(binder.getUserTO(uuid)).thenReturn(userTO);

        Method method = TestLogic.class.getMethod("otherOperation", Object.class);

        assertSame(userTO, logic.resolveReference(method, uuid));
    }

    @Test
    void unresolvedReference() throws Exception {
        Method method = TestLogic.class.getMethod("otherOperation", Object.class);

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference(method, 42));
    }

    @Test
    void securityChecksForUpdate() {
        UserUR userUR = new UserUR.Builder("key").build();
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.update(any(UserUR.class), eq(false), eq("unauthenticated"), eq("REST"))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));

        logic.update(userUR);

        verify(userDAO).securityChecks(any(), eq("key"), eq(SyncopeConstants.ROOT_REALM), any());
    }
}
