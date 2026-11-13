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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class AnyObjectLogicTest {

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private AnyObjectDAO anyObjectDAO;

    private AnySearchDAO searchDAO;

    private AnyObjectDataBinder binder;

    private AnyObjectProvisioningManager provisioningManager;

    private AnyObjectLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        anyObjectDAO = mock(AnyObjectDAO.class);
        searchDAO = mock(AnySearchDAO.class);
        binder = mock(AnyObjectDataBinder.class);
        provisioningManager = mock(AnyObjectProvisioningManager.class);

        logic = new AnyObjectLogic(
                realmSearchDAO,
                anyTypeDAO,
                mock(TemplateUtils.class),
                anyObjectDAO,
                searchDAO,
                binder,
                provisioningManager);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void read() {
        AnyObjectTO to = new AnyObjectTO();
        to.setKey("key");
        when(binder.getAnyObjectTO("key")).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void readByTypeAndNameMissing() {
        doReturn(Optional.empty()).when(anyObjectDAO).findKey("type", "missing");

        assertThrows(NotFoundException.class, () -> logic.read("type", "missing"));
    }

    @Test
    void readByTypeAndName() {
        AnyObjectTO to = new AnyObjectTO();
        to.setKey("key");

        doReturn(Optional.of("key")).when(anyObjectDAO).findKey("type", "name");
        when(binder.getAnyObjectTO("key")).thenReturn(to);

        assertSame(to, logic.read("type", "name"));
    }

    @Test
    void searchWithoutAnyType() {
        SearchCond searchCond = mock(SearchCond.class);

        assertThrows(UnsupportedOperationException.class,
                () -> logic.search(searchCond, Pageable.unpaged(), SyncopeConstants.ROOT_REALM, true, false));
    }

    @Test
    void searchMissingRealm() {
        SearchCond searchCond = mock(SearchCond.class);
        when(searchCond.hasAnyTypeCond()).thenReturn("type");
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class,
                () -> logic.search(searchCond, Pageable.unpaged(), "/missing", true, false));
    }

    @Test
    void search() {
        SearchCond searchCond = mock(SearchCond.class);
        when(searchCond.hasAnyTypeCond()).thenReturn("type");
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(searchDAO.count(eq(realm), eq(true), any(), eq(searchCond), any())).thenReturn(0L);
        when(searchDAO.search(eq(realm), eq(true), any(), eq(searchCond), any(Pageable.class), any())).
                thenReturn(List.of());

        assertEquals(0, logic.search(searchCond, Pageable.unpaged(),
                SyncopeConstants.ROOT_REALM, true, false).getTotalElements());
    }

    @Test
    void create() {
        AnyObjectCR createReq = new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM, "type", "name").build();
        Realm realm = mock(Realm.class);
        AnyType anyType = mock(AnyType.class);
        AnyObjectTO to = new AnyObjectTO();
        to.setKey("key");

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        doReturn(Optional.of(anyType)).when(anyTypeDAO).findById("type");
        when(provisioningManager.create(createReq, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("key", List.of()));
        when(binder.getAnyObjectTO("key")).thenReturn(to);

        assertSame(to, logic.create(createReq, false).getEntity());
    }

    @Test
    void delete() {
        AnyObjectTO to = new AnyObjectTO();
        to.setKey("key");
        to.setRealm(SyncopeConstants.ROOT_REALM);
        to.setType("type");
        Realm realm = mock(Realm.class);

        when(binder.getAnyObjectTO("key")).thenReturn(to);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.delete("key", false, "unauthenticated", "REST")).thenReturn(List.of());
        when(anyObjectDAO.existsById("key")).thenReturn(true);

        assertSame(to, logic.delete("key", false).getEntity());
    }

    @Test
    void unlink() {
        AnyObject anyObject = mock(AnyObject.class);
        AnyType anyType = mock(AnyType.class);
        Realm realm = mock(Realm.class);
        AnyObjectTO to = new AnyObjectTO();
        to.setKey("key");

        when(anyObject.getType()).thenReturn(anyType);
        when(anyType.getKey()).thenReturn("type");
        when(anyObject.getRealm()).thenReturn(realm);
        when(realm.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);
        when(anyObjectDAO.authFind("key")).thenReturn(anyObject);
        when(provisioningManager.unlink(any(AnyObjectUR.class), eq("unauthenticated"), eq("REST"))).thenReturn("key");
        when(binder.getAnyObjectTO("key")).thenReturn(to);

        assertSame(to, logic.unlink("key", List.of("resource")));
    }
}
