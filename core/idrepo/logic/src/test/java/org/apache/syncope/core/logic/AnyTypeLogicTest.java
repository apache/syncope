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
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

public class AnyTypeLogicTest {

    private AnyTypeDataBinder binder;

    private AnyTypeDAO anyTypeDAO;

    private AnyObjectDAO anyObjectDAO;

    private ApplicationEventPublisher publisher;

    private AnyTypeLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(AnyTypeDataBinder.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        anyObjectDAO = mock(AnyObjectDAO.class);
        publisher = mock(ApplicationEventPublisher.class);
        logic = new AnyTypeLogic(binder, anyTypeDAO, anyObjectDAO, publisher);
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(anyTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readAndList() {
        AnyType anyType = mock(AnyType.class);
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("key");

        doReturn(Optional.of(anyType)).when(anyTypeDAO).findById("key");
        doReturn(List.of(anyType)).when(anyTypeDAO).findAll();
        when(binder.getAnyTypeTO(anyType)).thenReturn(to);

        assertSame(to, logic.read("key"));
        assertEquals(List.of(to), logic.list());
    }

    @Test
    void createWithoutKey() {
        assertThrows(SyncopeClientException.class, () -> logic.create(new AnyTypeTO()));
    }

    @Test
    void createDuplicate() {
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("key");
        when(anyTypeDAO.existsById("key")).thenReturn(true);

        assertThrows(DuplicateException.class, () -> logic.create(to));
    }

    @Test
    void create() {
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("key");

        AnyType anyType = mock(AnyType.class);
        when(binder.create(to)).thenReturn(anyType);
        when(anyTypeDAO.save(anyType)).thenReturn(anyType);
        when(binder.getAnyTypeTO(anyType)).thenReturn(to);

        assertSame(to, logic.create(to));
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }

    @Test
    void updateMissing() {
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(anyTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("key");

        AnyType anyType = mock(AnyType.class);
        doReturn(Optional.of(anyType)).when(anyTypeDAO).findById("key");
        when(anyTypeDAO.save(anyType)).thenReturn(anyType);

        logic.update(to);

        verify(binder).update(anyType, to);
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(anyTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void deleteWithAnyObjects() {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("key");
        doReturn(Optional.of(anyType)).when(anyTypeDAO).findById("key");
        when(anyObjectDAO.countByType()).thenReturn(Map.of("key", 2L));

        assertThrows(SyncopeClientException.class, () -> logic.delete("key"));
    }

    @Test
    void delete() {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("key");
        AnyTypeTO to = new AnyTypeTO();
        to.setKey("key");

        doReturn(Optional.of(anyType)).when(anyTypeDAO).findById("key");
        when(anyObjectDAO.countByType()).thenReturn(Map.of());
        when(binder.delete(anyType)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }
}
