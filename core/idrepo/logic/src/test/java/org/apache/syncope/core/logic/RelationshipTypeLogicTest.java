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
import java.util.Optional;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

public class RelationshipTypeLogicTest {

    private RelationshipTypeDataBinder binder;

    private RelationshipTypeDAO relationshipTypeDAO;

    private ApplicationEventPublisher publisher;

    private RelationshipTypeLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(RelationshipTypeDataBinder.class);
        relationshipTypeDAO = mock(RelationshipTypeDAO.class);
        publisher = mock(ApplicationEventPublisher.class);
        logic = new RelationshipTypeLogic(binder, relationshipTypeDAO, publisher);
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(relationshipTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readAndList() {
        RelationshipType relationshipType = mock(RelationshipType.class);
        RelationshipTypeTO to = new RelationshipTypeTO();
        to.setKey("key");

        doReturn(Optional.of(relationshipType)).when(relationshipTypeDAO).findById("key");
        doReturn(List.of(relationshipType)).when(relationshipTypeDAO).findAll();
        when(binder.getRelationshipTypeTO(relationshipType)).thenReturn(to);

        assertSame(to, logic.read("key"));
        assertEquals(List.of(to), logic.list());
    }

    @Test
    void create() {
        RelationshipTypeTO to = new RelationshipTypeTO();
        to.setKey("key");

        RelationshipType relationshipType = mock(RelationshipType.class);
        when(binder.create(to)).thenReturn(relationshipType);
        when(relationshipTypeDAO.save(relationshipType)).thenReturn(relationshipType);
        when(binder.getRelationshipTypeTO(relationshipType)).thenReturn(to);

        assertSame(to, logic.create(to));
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }

    @Test
    void updateMissing() {
        RelationshipTypeTO to = new RelationshipTypeTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(relationshipTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        RelationshipTypeTO to = new RelationshipTypeTO();
        to.setKey("key");

        RelationshipType relationshipType = mock(RelationshipType.class);
        RelationshipType updated = mock(RelationshipType.class);
        doReturn(Optional.of(relationshipType)).when(relationshipTypeDAO).findById("key");
        when(binder.update(relationshipType, to)).thenReturn(updated);
        when(relationshipTypeDAO.save(updated)).thenReturn(updated);
        when(binder.getRelationshipTypeTO(updated)).thenReturn(to);

        assertSame(to, logic.update(to));
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(relationshipTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        RelationshipType relationshipType = mock(RelationshipType.class);
        RelationshipTypeTO to = new RelationshipTypeTO();
        to.setKey("key");

        doReturn(Optional.of(relationshipType)).when(relationshipTypeDAO).findById("key");
        when(binder.getRelationshipTypeTO(relationshipType)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(relationshipTypeDAO).deleteById("key");
        verify(publisher).publishEvent(any(EntityLifecycleEvent.class));
    }
}
