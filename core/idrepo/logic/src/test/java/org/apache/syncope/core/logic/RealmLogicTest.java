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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class RealmLogicTest {

    private RealmDAO realmDAO;

    private RealmSearchDAO realmSearchDAO;

    private AnySearchDAO searchDAO;

    private RealmDataBinder binder;

    private RealmLogic logic;

    @BeforeEach
    void setUp() {
        realmDAO = mock(RealmDAO.class);
        realmSearchDAO = mock(RealmSearchDAO.class);
        searchDAO = mock(AnySearchDAO.class);
        binder = mock(RealmDataBinder.class);

        logic = new RealmLogic(
                realmDAO,
                realmSearchDAO,
                searchDAO,
                mock(TaskDAO.class),
                mock(CASSPClientAppDAO.class),
                mock(OIDCRPClientAppDAO.class),
                mock(SAML2SPClientAppDAO.class),
                binder,
                mock(PropagationManager.class),
                mock(PropagationTaskExecutor.class));
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void search() {
        when(realmSearchDAO.getAllMatchingCond()).thenReturn(mock(SearchCond.class));
        when(realmSearchDAO.count(any(), any(SearchCond.class))).thenReturn(0L);
        when(realmSearchDAO.search(any(), any(SearchCond.class), any(Pageable.class))).thenReturn(List.of());

        assertEquals(0, logic.search(Set.of(), null, Pageable.unpaged()).getTotalElements());
    }

    @Test
    void searchWithMissingBase() {
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class,
                () -> logic.search(Set.of("/missing"), mock(SearchCond.class), Pageable.unpaged()));
    }

    @Test
    void createWithMissingParent() {
        RealmTO to = new RealmTO();
        to.setName("child");
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class, () -> logic.create("/missing", to));
    }

    @Test
    void createWithMismatchingParent() {
        RealmTO to = new RealmTO();
        to.setName("child");
        to.setParent("parent-key");

        Realm parent = mock(Realm.class);
        when(parent.getFullPath()).thenReturn("/parent");
        doReturn(Optional.of(parent)).when(realmDAO).findById("parent-key");

        assertThrows(SyncopeClientException.class, () -> logic.create("/other", to));
    }

    @Test
    void createDuplicate() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.REALM_CREATE, Set.of(SyncopeConstants.ROOT_REALM)));

        RealmTO to = new RealmTO();
        to.setName("child");

        Realm parent = mock(Realm.class);
        when(parent.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);
        doReturn(Optional.of(parent)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        doReturn(Optional.of(mock(Realm.class))).when(realmSearchDAO).findByFullPath("/child");

        assertThrows(DuplicateException.class, () -> logic.create(SyncopeConstants.ROOT_REALM, to));
    }

    @Test
    void deleteWithChildren() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.REALM_DELETE, Set.of(SyncopeConstants.ROOT_REALM)));

        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/realm");
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath("/realm");
        when(realmSearchDAO.findChildren(realm)).thenReturn(List.of(mock(Realm.class)));

        assertThrows(SyncopeClientException.class, () -> logic.delete("/realm"));
    }

    @Test
    void deleteWithUsers() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.REALM_DELETE, Set.of(SyncopeConstants.ROOT_REALM)));

        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/realm");
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath("/realm");
        when(realmSearchDAO.findChildren(realm)).thenReturn(List.of());
        when(searchDAO.count(any(), any(Boolean.class), any(), any(SearchCond.class), any())).thenReturn(1L);

        assertThrows(SyncopeClientException.class, () -> logic.delete("/realm"));
    }
}
