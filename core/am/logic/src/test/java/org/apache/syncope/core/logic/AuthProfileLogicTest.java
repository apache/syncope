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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthProfileLogicTest {

    private AuthProfileDataBinder binder;

    private AuthProfileDAO authProfileDAO;

    private EntityFactory entityFactory;

    private AuthProfileLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(AuthProfileDataBinder.class);
        authProfileDAO = mock(AuthProfileDAO.class);
        entityFactory = mock(EntityFactory.class);
        logic = new AuthProfileLogic(binder, authProfileDAO, entityFactory);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        when(authProfileDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void read() {
        AuthProfile profile = mock(AuthProfile.class);
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");

        doReturn(Optional.of(profile)).when(authProfileDAO).findById("key");
        when(binder.getAuthProfileTO(profile)).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void delete() {
        logic.delete("key");

        verify(authProfileDAO).deleteById("key");
    }

    @Test
    void create() {
        AuthProfileTO to = new AuthProfileTO();
        AuthProfile profile = mock(AuthProfile.class);

        when(binder.create(to)).thenReturn(profile);
        when(authProfileDAO.save(profile)).thenReturn(profile);
        when(binder.getAuthProfileTO(profile)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");

        when(authProfileDAO.findById("key")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }
}
