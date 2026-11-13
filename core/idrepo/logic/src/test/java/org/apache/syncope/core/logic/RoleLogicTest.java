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
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleLogicTest {

    private RoleDataBinder binder;

    private RoleDAO roleDAO;

    private RoleLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(RoleDataBinder.class);
        roleDAO = mock(RoleDAO.class);
        logic = new RoleLogic(binder, roleDAO);
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(roleDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readAndList() {
        Role role = mock(Role.class);
        RoleTO to = new RoleTO();
        to.setKey("key");

        doReturn(Optional.of(role)).when(roleDAO).findById("key");
        doReturn(List.of(role)).when(roleDAO).findAll();
        when(binder.getRoleTO(role)).thenReturn(to);

        assertSame(to, logic.read("key"));
        assertEquals(List.of(to), logic.list());
    }

    @Test
    void create() {
        RoleTO to = new RoleTO();
        to.setKey("key");
        Role role = mock(Role.class);

        when(binder.create(to)).thenReturn(role);
        when(binder.getRoleTO(role)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        RoleTO to = new RoleTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(roleDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        RoleTO to = new RoleTO();
        to.setKey("key");
        Role role = mock(Role.class);
        Role updated = mock(Role.class);

        doReturn(Optional.of(role)).when(roleDAO).findById("key");
        when(binder.update(role, to)).thenReturn(updated);
        when(binder.getRoleTO(updated)).thenReturn(to);

        assertSame(to, logic.update(to));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(roleDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        Role role = mock(Role.class);
        RoleTO to = new RoleTO();
        to.setKey("key");

        doReturn(Optional.of(role)).when(roleDAO).findById("key");
        when(binder.getRoleTO(role)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(roleDAO).delete(role);
    }

    @Test
    void getAnyLayout() {
        Role role = mock(Role.class);
        doReturn(Optional.of(role)).when(roleDAO).findById("key");
        when(role.getAnyLayout()).thenReturn("layout");

        assertEquals("layout", logic.getAnyLayout("key"));
    }

    @Test
    void getAnyLayoutMissing() {
        Role role = mock(Role.class);
        doReturn(Optional.of(role)).when(roleDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.getAnyLayout("key"));
    }

    @Test
    void setAnyLayout() {
        Role role = mock(Role.class);
        doReturn(Optional.of(role)).when(roleDAO).findById("key");

        logic.setAnyLayout("key", "layout");

        verify(role).setAnyLayout("layout");
        verify(roleDAO).save(role);
    }
}
