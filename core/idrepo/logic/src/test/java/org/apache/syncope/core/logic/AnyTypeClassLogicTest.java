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
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnyTypeClassLogicTest {

    private AnyTypeClassDataBinder binder;

    private AnyTypeClassDAO anyTypeClassDAO;

    private AnyTypeClassLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(AnyTypeClassDataBinder.class);
        anyTypeClassDAO = mock(AnyTypeClassDAO.class);
        logic = new AnyTypeClassLogic(binder, anyTypeClassDAO);
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(anyTypeClassDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readAndList() {
        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");

        doReturn(Optional.of(anyTypeClass)).when(anyTypeClassDAO).findById("key");
        doReturn(List.of(anyTypeClass)).when(anyTypeClassDAO).findAll();
        when(binder.getAnyTypeClassTO(anyTypeClass)).thenReturn(to);

        assertSame(to, logic.read("key"));
        assertEquals(List.of(to), logic.list());
    }

    @Test
    void createWithoutKey() {
        assertThrows(SyncopeClientException.class, () -> logic.create(new AnyTypeClassTO()));
    }

    @Test
    void createDuplicate() {
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");
        when(anyTypeClassDAO.existsById("key")).thenReturn(true);

        assertThrows(DuplicateException.class, () -> logic.create(to));
    }

    @Test
    void create() {
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");

        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        when(binder.create(to)).thenReturn(anyTypeClass);
        when(anyTypeClassDAO.save(anyTypeClass)).thenReturn(anyTypeClass);
        when(binder.getAnyTypeClassTO(anyTypeClass)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(anyTypeClassDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");

        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        AnyTypeClass updated = mock(AnyTypeClass.class);
        doReturn(Optional.of(anyTypeClass)).when(anyTypeClassDAO).findById("key");
        when(binder.update(anyTypeClass, to)).thenReturn(updated);
        when(anyTypeClassDAO.save(updated)).thenReturn(updated);
        when(binder.getAnyTypeClassTO(updated)).thenReturn(to);

        assertSame(to, logic.update(to));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(anyTypeClassDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");

        doReturn(Optional.of(anyTypeClass)).when(anyTypeClassDAO).findById("key");
        when(binder.getAnyTypeClassTO(anyTypeClass)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(anyTypeClassDAO).deleteById("key");
    }

    @Test
    void resolveBeanReference() throws Exception {
        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        AnyTypeClassTO to = new AnyTypeClassTO();
        to.setKey("key");

        doReturn(Optional.of(anyTypeClass)).when(anyTypeClassDAO).findById("key");
        when(binder.getAnyTypeClassTO(anyTypeClass)).thenReturn(to);

        assertSame(to, logic.resolveBeanReference(
                AnyTypeClassLogic.class.getMethod("create", AnyTypeClassTO.class), "key"));
    }
}
