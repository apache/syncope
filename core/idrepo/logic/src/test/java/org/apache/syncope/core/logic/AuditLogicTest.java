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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class AuditLogicTest {

    private AuditConfDAO auditConfDAO;

    private AuditEventDAO auditEventDAO;

    private EntityFactory entityFactory;

    private AuditDataBinder binder;

    private AuditManager auditManager;

    private AuditLogic logic;

    @BeforeEach
    void setUp() {
        auditConfDAO = mock(AuditConfDAO.class);
        auditEventDAO = mock(AuditEventDAO.class);
        entityFactory = mock(EntityFactory.class);
        binder = mock(AuditDataBinder.class);
        auditManager = mock(AuditManager.class);

        logic = new AuditLogic(
                auditConfDAO,
                auditEventDAO,
                mock(ExternalResourceDAO.class),
                entityFactory,
                mock(ImplementationLookup.class),
                binder,
                auditManager);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void confs() {
        AuditConf auditConf = mock(AuditConf.class);
        AuditConfTO to = new AuditConfTO();
        to.setKey("key");

        doReturn(List.of(auditConf)).when(auditConfDAO).findAll();
        when(binder.getAuditConfTO(auditConf)).thenReturn(to);

        assertEquals(List.of(to), logic.confs());
    }

    @Test
    void getConfMissing() {
        doReturn(Optional.empty()).when(auditConfDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.getConf("missing"));
    }

    @Test
    void getConf() {
        AuditConf auditConf = mock(AuditConf.class);
        AuditConfTO to = new AuditConfTO();
        to.setKey("key");

        doReturn(Optional.of(auditConf)).when(auditConfDAO).findById("key");
        when(binder.getAuditConfTO(auditConf)).thenReturn(to);

        assertSame(to, logic.getConf("key"));
    }

    @Test
    void setConf() {
        AuditConfTO to = new AuditConfTO();
        to.setKey("key");
        to.setActive(true);

        AuditConf auditConf = mock(AuditConf.class);
        doReturn(Optional.empty()).when(auditConfDAO).findById("key");
        when(entityFactory.newEntity(AuditConf.class)).thenReturn(auditConf);

        logic.setConf(to);

        verify(auditConf).setKey("key");
        verify(auditConf).setActive(true);
        verify(auditConfDAO).save(auditConf);
    }

    @Test
    void deleteConfMissing() {
        doReturn(Optional.empty()).when(auditConfDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.deleteConf("missing"));
    }

    @Test
    void deleteConf() {
        AuditConf auditConf = mock(AuditConf.class);
        doReturn(Optional.of(auditConf)).when(auditConfDAO).findById("key");

        logic.deleteConf("key");

        verify(auditConfDAO).delete(auditConf);
    }

    @Test
    void search() {
        Pageable pageable = Pageable.unpaged();
        when(auditEventDAO.count(
                null, Set.of(), Set.of(), null, null, null, null, null, null, null)).thenReturn(0L);
        when(auditEventDAO.search(
                null, Set.of(), Set.of(), null, null, null, null, null, null, null, pageable)).
                thenReturn(List.of());

        assertEquals(0, logic.search(
                null, Set.of(), Set.of(), null, null, null, null, null, null, null, pageable).
                getTotalElements());
    }

    @Test
    void createUnauthorized() {
        AuditEventTO to = new AuditEventTO();
        OpEvent opEvent = mock(OpEvent.class);
        when(opEvent.getType()).thenReturn(OpEvent.CategoryType.LOGIC);
        to.setOpEvent(opEvent);

        assertThrows(SyncopeClientException.class, () -> logic.create(to));
    }

    @Test
    void create() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdRepoEntitlement.AUDIT_SET, Set.of(SyncopeConstants.ROOT_REALM)));

        AuditEventTO to = new AuditEventTO();
        OpEvent opEvent = mock(OpEvent.class);
        when(opEvent.getType()).thenReturn(OpEvent.CategoryType.LOGIC);
        to.setOpEvent(opEvent);

        logic.create(to);

        verify(auditManager).audit(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
