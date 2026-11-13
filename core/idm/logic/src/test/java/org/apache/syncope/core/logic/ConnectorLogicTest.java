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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectorLogicTest {

    private ConnIdBundleManager connIdBundleManager;

    private ConnectorManager connectorManager;

    private ExternalResourceDAO resourceDAO;

    private ConnInstanceDAO connInstanceDAO;

    private ConnInstanceDataBinder binder;

    private ConnectorLogic logic;

    @BeforeEach
    void setUp() {
        connIdBundleManager = mock(ConnIdBundleManager.class);
        connectorManager = mock(ConnectorManager.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        connInstanceDAO = mock(ConnInstanceDAO.class);
        binder = mock(ConnInstanceDataBinder.class);

        logic = new ConnectorLogic(
                connIdBundleManager, connectorManager, resourceDAO, connInstanceDAO, binder);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        when(connInstanceDAO.authFind("missing")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> logic.read("missing", null));
    }

    @Test
    void deleteMissing() {
        when(connInstanceDAO.authFind("missing")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void deleteAssociatedResources() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdMEntitlement.CONNECTOR_DELETE, Set.of(SyncopeConstants.ROOT_REALM)));

        ConnInstance connInstance = mock(ConnInstance.class);
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource-1");

        var adminRealm = mock(Realm.class);
        when(adminRealm.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);

        when(connInstanceDAO.authFind("key")).thenReturn(connInstance);
        when(connInstance.getAdminRealm()).thenReturn(adminRealm);
        when(connInstance.getKey()).thenReturn("key");
        when(resourceDAO.findByConnInstance("key")).thenReturn(List.of(resource));

        assertThrows(SyncopeClientException.class, () -> logic.delete("key"));
    }

    @Test
    void list() {
        ConnInstance connInstance = mock(ConnInstance.class);
        ConnInstanceTO to = new ConnInstanceTO();
        doReturn(List.of(connInstance)).when(connInstanceDAO).findAll();
        when(binder.getConnInstanceTO(connInstance)).thenReturn(to);

        assertEquals(List.of(to), logic.list(null));
    }

    @Test
    void readByResourceMissing() {
        when(resourceDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.readByResource("missing", null));
    }
}
