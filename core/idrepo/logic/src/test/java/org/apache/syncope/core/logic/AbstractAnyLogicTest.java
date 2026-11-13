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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.api.LogicActions;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractAnyLogicTest {

    private static class TestLogic extends AbstractAnyLogic<UserTO, UserCR, UserUR> {

        TestLogic(
                final RealmSearchDAO realmSearchDAO,
                final AnyTypeDAO anyTypeDAO,
                final TemplateUtils templateUtils) {

            super(realmSearchDAO, anyTypeDAO, templateUtils);
        }

        BeforeResult<UserCR> beforeCreateUser(final UserCR input) {
            return beforeCreate(input);
        }

        BeforeResult<UserUR> beforeUpdateUser(final UserUR input, final String realmPath) {
            return beforeUpdate(input, realmPath);
        }

        BeforeResult<UserTO> beforeDeleteUser(final UserTO input) {
            return beforeDelete(input);
        }

        ProvisioningResult<UserTO> afterCreateUser(
                final UserTO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

            return afterCreate(input, statuses, actions);
        }

        ProvisioningResult<UserTO> afterUpdateUser(
                final UserTO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

            return afterUpdate(input, statuses, actions);
        }

        ProvisioningResult<UserTO> afterDeleteUser(
                final UserTO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

            return afterDelete(input, statuses, actions);
        }

        List<LogicActions> actions(final Realm realm) {
            return getActions(realm);
        }

        @Override
        protected UserTO resolveReference(final java.lang.reflect.Method method, final Object... args)
                throws UnresolvedReferenceException {

            throw new UnresolvedReferenceException();
        }
    }

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private TemplateUtils templateUtils;

    private TestLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        templateUtils = mock(TemplateUtils.class);
        logic = new TestLogic(realmSearchDAO, anyTypeDAO, templateUtils);
    }

    @Test
    void beforeCreateMissingRealm() {
        UserCR userCR = new UserCR.Builder("/missing", "user").build();
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(SyncopeClientException.class, () -> logic.beforeCreateUser(userCR));
    }

    @Test
    void beforeCreateMissingAnyType() {
        UserCR userCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anyTypeDAO.getUser()).thenReturn(null);

        assertThrows(SyncopeClientException.class, () -> logic.beforeCreateUser(userCR));
    }

    @Test
    void beforeCreateUser() {
        UserCR userCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();
        Realm realm = mock(Realm.class);
        AnyType anyType = mock(AnyType.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anyTypeDAO.getUser()).thenReturn(anyType);
        doReturn(Optional.empty()).when(realm).getTemplate(anyType);
        when(realm.getActions()).thenReturn(List.of());

        var result = logic.beforeCreateUser(userCR);

        assertSame(userCR, result.key());
        assertEquals(List.of(), result.actions());
    }

    @Test
    void beforeUpdateMissingRealm() {
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(SyncopeClientException.class,
                () -> logic.beforeUpdateUser(new UserUR.Builder("key").build(), "/missing"));
    }

    @Test
    void beforeUpdate() {
        UserUR userUR = new UserUR.Builder("key").build();
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());

        var result = logic.beforeUpdateUser(userUR, SyncopeConstants.ROOT_REALM);

        assertSame(userUR, result.key());
        assertEquals(List.of(), result.actions());
    }

    @Test
    void beforeDeleteMissingRealm() {
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm("/missing");
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(SyncopeClientException.class, () -> logic.beforeDeleteUser(userTO));
    }

    @Test
    void beforeDelete() {
        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());

        var result = logic.beforeDeleteUser(userTO);

        assertSame(userTO, result.key());
    }

    @Test
    void afterCreate() {
        UserTO input = new UserTO();
        input.setKey("key");
        UserTO output = new UserTO();
        output.setKey("output");

        LogicActions action = mock(LogicActions.class);
        doReturn(output).when(action).afterCreate(any(), any());
        PropagationStatus status = mock(PropagationStatus.class);

        ProvisioningResult<UserTO> result = logic.afterCreateUser(input, List.of(status), List.of(action));

        assertSame(output, result.getEntity());
        assertEquals(List.of(status), result.getPropagationStatuses());
    }

    @Test
    void afterUpdate() {
        UserTO input = new UserTO();
        input.setKey("key");

        LogicActions action = mock(LogicActions.class);
        doReturn(input).when(action).afterUpdate(input, List.of());

        ProvisioningResult<UserTO> result = logic.afterUpdateUser(input, List.of(), List.of(action));

        assertSame(input, result.getEntity());
    }

    @Test
    void afterDelete() {
        UserTO input = new UserTO();
        input.setKey("key");

        LogicActions action = mock(LogicActions.class);
        doReturn(input).when(action).afterDelete(input, List.of());

        ProvisioningResult<UserTO> result = logic.afterDeleteUser(input, List.of(), List.of(action));

        assertSame(input, result.getEntity());
    }

    @Test
    void getActions() {
        Realm realm = mock(Realm.class);
        when(realm.getActions()).thenReturn(List.of());

        assertEquals(List.of(), logic.actions(realm));
    }
}
