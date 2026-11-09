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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaLogicTest {

    private PlainSchemaDAO plainSchemaDAO;

    private DerSchemaDAO derSchemaDAO;

    private AnyTypeClassDAO anyTypeClassDAO;

    private ImplementationDAO implementationDAO;

    private SchemaDataBinder binder;

    private SchemaLogic logic;

    @BeforeEach
    void setUp() {
        plainSchemaDAO = mock(PlainSchemaDAO.class);
        derSchemaDAO = mock(DerSchemaDAO.class);
        anyTypeClassDAO = mock(AnyTypeClassDAO.class);
        implementationDAO = mock(ImplementationDAO.class);
        binder = mock(SchemaDataBinder.class);

        logic = new SchemaLogic(plainSchemaDAO, derSchemaDAO, anyTypeClassDAO, implementationDAO, binder);
    }

    @Test
    void createWithoutKey() {
        assertThrows(SyncopeClientException.class, () -> logic.create(SchemaType.PLAIN, new PlainSchemaTO()));
    }

    @Test
    void createDuplicate() {
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("key");
        doReturn(Optional.of(mock(PlainSchema.class))).when(plainSchemaDAO).findById("key");

        assertThrows(DuplicateException.class, () -> logic.create(SchemaType.PLAIN, to));
    }

    @Test
    void createPlain() {
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("key");

        PlainSchema schema = mock(PlainSchema.class);
        when(schema.getKey()).thenReturn("key");
        when(binder.create(to)).thenReturn(schema);
        when(binder.getPlainSchemaTO("key")).thenReturn(to);

        assertSame(to, logic.create(SchemaType.PLAIN, to));
    }

    @Test
    void createDerived() {
        DerSchemaTO to = new DerSchemaTO();
        to.setKey("key");

        DerSchema schema = mock(DerSchema.class);
        when(schema.getKey()).thenReturn("key");
        when(binder.create(to)).thenReturn(schema);
        when(binder.getDerSchemaTO("key")).thenReturn(to);

        assertSame(to, logic.create(SchemaType.DERIVED, to));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(plainSchemaDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete(SchemaType.PLAIN, "missing"));
    }

    @Test
    void deletePlain() {
        doReturn(Optional.of(mock(PlainSchema.class))).when(plainSchemaDAO).findById("key");

        logic.delete(SchemaType.PLAIN, "key");

        verify(plainSchemaDAO).deleteById("key");
    }

    @Test
    void deleteDerived() {
        doReturn(Optional.of(mock(DerSchema.class))).when(derSchemaDAO).findById("key");

        logic.delete(SchemaType.DERIVED, "key");

        verify(derSchemaDAO).deleteById("key");
    }

    @Test
    void searchPlain() {
        PlainSchema schema = mock(PlainSchema.class);
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("key");

        doReturn(List.of(schema)).when(plainSchemaDAO).findAll();
        when(schema.getKey()).thenReturn("key");
        when(binder.getPlainSchemaTO("key")).thenReturn(to);

        assertEquals(List.of(to), logic.search(SchemaType.PLAIN, new ArrayList<>(), null));
    }

    @Test
    void searchDerived() {
        DerSchema schema = mock(DerSchema.class);
        DerSchemaTO to = new DerSchemaTO();
        to.setKey("key");

        doReturn(List.of(schema)).when(derSchemaDAO).findAll();
        when(schema.getKey()).thenReturn("key");
        when(binder.getDerSchemaTO("key")).thenReturn(to);

        assertEquals(List.of(to), logic.search(SchemaType.DERIVED, new ArrayList<>(), null));
    }

    @Test
    void read() {
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("key");
        when(binder.getPlainSchemaTO("key")).thenReturn(to);

        assertSame(to, logic.read(SchemaType.PLAIN, "key"));
    }

    @Test
    void updateMissing() {
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(plainSchemaDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(SchemaType.PLAIN, to));
    }

    @Test
    void update() {
        PlainSchemaTO to = new PlainSchemaTO();
        to.setKey("key");
        PlainSchema schema = mock(PlainSchema.class);
        doReturn(Optional.of(schema)).when(plainSchemaDAO).findById("key");

        logic.update(SchemaType.PLAIN, to);

        verify(binder).update(to, schema);
    }

    @Test
    void resolveBeanReference() throws Exception {
        DerSchemaTO to = new DerSchemaTO();
        to.setKey("key");
        DerSchema schema = mock(DerSchema.class);

        doReturn(Optional.empty()).when(plainSchemaDAO).findById("key");
        doReturn(Optional.of(schema)).when(derSchemaDAO).findById("key");
        when(binder.getDerSchemaTO("key")).thenReturn(to);

        assertSame(to, logic.resolveBeanReference(
                SchemaLogic.class.getMethod("create", SchemaType.class, SchemaTO.class),
                to));
    }
}
