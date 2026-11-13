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
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImplementationLogicTest {

    private ImplementationDataBinder binder;

    private ImplementationDAO implementationDAO;

    private ReportDAO reportDAO;

    private PolicyDAO policyDAO;

    private ExternalResourceDAO resourceDAO;

    private TaskDAO taskDAO;

    private RealmDAO realmDAO;

    private PlainSchemaDAO plainSchemaDAO;

    private NotificationDAO notificationDAO;

    private ImplementationLogic logic;

    @BeforeEach
    void setUp() {
        ImplementationTypesHolder.getInstance().putAll(IdRepoImplementationType.values());

        binder = mock(ImplementationDataBinder.class);
        implementationDAO = mock(ImplementationDAO.class);
        reportDAO = mock(ReportDAO.class);
        policyDAO = mock(PolicyDAO.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        taskDAO = mock(TaskDAO.class);
        realmDAO = mock(RealmDAO.class);
        plainSchemaDAO = mock(PlainSchemaDAO.class);
        notificationDAO = mock(NotificationDAO.class);

        logic = new ImplementationLogic(
                binder,
                implementationDAO,
                reportDAO,
                policyDAO,
                resourceDAO,
                taskDAO,
                realmDAO,
                plainSchemaDAO,
                notificationDAO);
    }

    @Test
    void listWithInvalidType() {
        assertThrows(SyncopeClientException.class, () -> logic.list("unknown"));
    }

    @Test
    void list() {
        Implementation implementation = mock(Implementation.class);
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");

        doReturn(List.of(implementation)).when(implementationDAO).findByType(IdRepoImplementationType.COMMAND);
        when(binder.getImplementationTO(implementation)).thenReturn(to);

        assertEquals(List.of(to), logic.list(IdRepoImplementationType.COMMAND));
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(implementationDAO).findById("missing");

        assertThrows(NotFoundException.class,
                () -> logic.read(IdRepoImplementationType.COMMAND, "missing"));
    }

    @Test
    void readWithWrongType() {
        Implementation implementation = mock(Implementation.class);
        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementation.getType()).thenReturn(IdRepoImplementationType.TASKJOB_DELEGATE);

        assertThrows(SyncopeClientException.class,
                () -> logic.read(IdRepoImplementationType.COMMAND, "key"));
    }

    @Test
    void read() {
        Implementation implementation = mock(Implementation.class);
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");

        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementation.getType()).thenReturn(IdRepoImplementationType.COMMAND);
        when(binder.getImplementationTO(implementation)).thenReturn(to);

        assertSame(to, logic.read(IdRepoImplementationType.COMMAND, "key"));
    }

    @Test
    void createWithoutKey() {
        assertThrows(SyncopeClientException.class, () -> logic.create(new ImplementationTO()));
    }

    @Test
    void createDuplicate() {
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");
        to.setType(IdRepoImplementationType.COMMAND);
        when(implementationDAO.existsById("key")).thenReturn(true);

        assertThrows(DuplicateException.class, () -> logic.create(to));
    }

    @Test
    void createWithInvalidType() {
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");
        to.setType("unknown");

        assertThrows(SyncopeClientException.class, () -> logic.create(to));
    }

    @Test
    void create() {
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");
        to.setType(IdRepoImplementationType.COMMAND);

        Implementation implementation = mock(Implementation.class);
        when(binder.create(to)).thenReturn(implementation);
        when(implementationDAO.save(implementation)).thenReturn(implementation);
        when(binder.getImplementationTO(implementation)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        ImplementationTO to = new ImplementationTO();
        to.setKey("missing");
        to.setType(IdRepoImplementationType.COMMAND);
        doReturn(Optional.empty()).when(implementationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");
        to.setType(IdRepoImplementationType.COMMAND);

        Implementation implementation = mock(Implementation.class);
        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementationDAO.save(implementation)).thenReturn(implementation);
        when(binder.getImplementationTO(implementation)).thenReturn(to);

        assertSame(to, logic.update(to));
        verify(binder).update(implementation, to);
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(implementationDAO).findById("missing");

        assertThrows(NotFoundException.class,
                () -> logic.delete(IdRepoImplementationType.COMMAND, "missing"));
    }

    @Test
    void deleteWithWrongType() {
        Implementation implementation = mock(Implementation.class);
        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementation.getType()).thenReturn(IdRepoImplementationType.TASKJOB_DELEGATE);

        assertThrows(SyncopeClientException.class,
                () -> logic.delete(IdRepoImplementationType.COMMAND, "key"));
    }

    @Test
    void deleteInUse() {
        Implementation implementation = mock(Implementation.class);
        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementation.getType()).thenReturn(IdRepoImplementationType.ACCOUNT_RULE);
        doReturn(List.of(mock(org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy.class))).
                when(policyDAO).findByAccountRule(implementation);

        assertThrows(SyncopeClientException.class,
                () -> logic.delete(IdRepoImplementationType.ACCOUNT_RULE, "key"));
    }

    @Test
    void delete() {
        Implementation implementation = mock(Implementation.class);
        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(implementation.getType()).thenReturn("custom");

        logic.delete("custom", "key");

        verify(implementationDAO).deleteById("key");
    }

    @Test
    void resolveBeanReference() throws Exception {
        Implementation implementation = mock(Implementation.class);
        ImplementationTO to = new ImplementationTO();
        to.setKey("key");

        doReturn(Optional.of(implementation)).when(implementationDAO).findById("key");
        when(binder.getImplementationTO(implementation)).thenReturn(to);

        assertSame(to, logic.resolveBeanReference(
                ImplementationLogic.class.getMethod("create", ImplementationTO.class), "key"));
    }
}
