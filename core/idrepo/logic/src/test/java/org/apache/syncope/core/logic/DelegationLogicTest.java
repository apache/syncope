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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DelegationLogicTest {

    private static final String DELEGATING = "11111111-1111-1111-1111-111111111111";

    private DelegationDataBinder binder;

    private DelegationDAO delegationDAO;

    private UserDAO userDAO;

    private DelegationLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(DelegationDataBinder.class);
        delegationDAO = mock(DelegationDAO.class);
        userDAO = mock(UserDAO.class);
        logic = new DelegationLogic(binder, delegationDAO, userDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(delegationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void read() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.DELEGATION_READ, Set.of(SyncopeConstants.ROOT_REALM)));

        Delegation delegation = mock(Delegation.class);
        User delegating = mock(User.class);
        DelegationTO to = new DelegationTO();
        to.setKey("key");

        when(delegation.getDelegating()).thenReturn(delegating);
        when(delegating.getKey()).thenReturn(DELEGATING);
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("key");
        when(binder.getDelegationTO(delegation)).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void listWithoutListEntitlement() {
        doReturn(Optional.empty()).when(userDAO).findByUsername("unauthenticated");

        assertThrows(NotFoundException.class, () -> logic.list());
    }

    @Test
    void listWithListEntitlement() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.DELEGATION_LIST, Set.of(SyncopeConstants.ROOT_REALM)));

        Delegation delegation = mock(Delegation.class);
        DelegationTO to = new DelegationTO();
        to.setKey("key");

        doReturn(List.of(delegation)).when(delegationDAO).findAll();
        when(binder.getDelegationTO(delegation)).thenReturn(to);

        assertEquals(List.of(to), logic.list());
    }

    @Test
    void create() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.DELEGATION_CREATE, Set.of(SyncopeConstants.ROOT_REALM)));

        DelegationTO to = new DelegationTO();
        to.setKey("key");
        to.setDelegating(DELEGATING);

        Delegation delegation = mock(Delegation.class);
        when(binder.create(to)).thenReturn(delegation);
        when(delegationDAO.save(delegation)).thenReturn(delegation);
        when(binder.getDelegationTO(delegation)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        DelegationTO to = new DelegationTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(delegationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.DELEGATION_UPDATE, Set.of(SyncopeConstants.ROOT_REALM)));

        DelegationTO to = new DelegationTO();
        to.setKey("key");
        to.setDelegating(DELEGATING);

        Delegation delegation = mock(Delegation.class);
        User delegating = mock(User.class);
        when(delegating.getKey()).thenReturn(DELEGATING);
        when(delegation.getDelegating()).thenReturn(delegating);
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("key");
        when(binder.update(delegation, to)).thenReturn(delegation);
        when(delegationDAO.save(delegation)).thenReturn(delegation);
        when(binder.getDelegationTO(delegation)).thenReturn(to);

        assertSame(to, logic.update(to));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(delegationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.DELEGATION_DELETE, Set.of(SyncopeConstants.ROOT_REALM)));

        Delegation delegation = mock(Delegation.class);
        User delegating = mock(User.class);
        DelegationTO to = new DelegationTO();
        to.setKey("key");

        when(delegating.getKey()).thenReturn(DELEGATING);
        when(delegation.getDelegating()).thenReturn(delegating);
        doReturn(Optional.of(delegation)).when(delegationDAO).findById("key");
        when(binder.getDelegationTO(delegation)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(delegationDAO).deleteById("key");
    }
}
