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
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FIQLQueryLogicTest {

    private FIQLQueryDataBinder binder;

    private FIQLQueryDAO fiqlQueryDAO;

    private UserDAO userDAO;

    private SecurityProperties securityProperties;

    private FIQLQueryLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(FIQLQueryDataBinder.class);
        fiqlQueryDAO = mock(FIQLQueryDAO.class);
        userDAO = mock(UserDAO.class);

        securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");
        securityProperties.setAnonymousUser("anonymous");

        logic = new FIQLQueryLogic(binder, fiqlQueryDAO, userDAO, securityProperties);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void adminIsSkipped() {
        LogicTestAuth.authenticate("admin");

        assertThrows(NotFoundException.class, () -> logic.read("key"));
        assertEquals(List.of(), logic.list(AnyTypeKind.USER.name()));
        assertThrows(NotFoundException.class, () -> logic.create(new FIQLQueryTO()));
    }

    @Test
    void anonymousIsSkipped() {
        LogicTestAuth.authenticate("anonymous");

        assertThrows(NotFoundException.class, () -> logic.read("key"));
        assertEquals(List.of(), logic.list(AnyTypeKind.USER.name()));
    }

    @Test
    void readMissing() {
        LogicTestAuth.authenticate("user");
        doReturn(Optional.empty()).when(fiqlQueryDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readWithDifferentOwner() {
        LogicTestAuth.authenticate("user");

        FIQLQuery fiqlQuery = mock(FIQLQuery.class);
        User owner = mock(User.class);
        when(owner.getUsername()).thenReturn("other");
        when(fiqlQuery.getOwner()).thenReturn(owner);
        doReturn(Optional.of(fiqlQuery)).when(fiqlQueryDAO).findById("key");

        assertThrows(DelegatedAdministrationException.class, () -> logic.read("key"));
    }

    @Test
    void read() {
        LogicTestAuth.authenticate("user");

        FIQLQuery fiqlQuery = mock(FIQLQuery.class);
        User owner = mock(User.class);
        FIQLQueryTO to = new FIQLQueryTO();
        to.setKey("key");

        when(owner.getUsername()).thenReturn("user");
        when(fiqlQuery.getOwner()).thenReturn(owner);
        doReturn(Optional.of(fiqlQuery)).when(fiqlQueryDAO).findById("key");
        when(binder.getFIQLQueryTO(fiqlQuery)).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void listMissingUser() {
        LogicTestAuth.authenticate("user");
        doReturn(Optional.empty()).when(userDAO).findByUsername("user");

        assertThrows(NotFoundException.class, () -> logic.list(AnyTypeKind.USER.name()));
    }

    @Test
    void list() {
        LogicTestAuth.authenticate("user");

        User user = mock(User.class);
        FIQLQuery fiqlQuery = mock(FIQLQuery.class);
        FIQLQueryTO to = new FIQLQueryTO();
        to.setKey("key");

        doReturn(Optional.of(user)).when(userDAO).findByUsername("user");
        doReturn(List.of(fiqlQuery)).when(fiqlQueryDAO).findByOwner(user, AnyTypeKind.USER.name());
        when(binder.getFIQLQueryTO(fiqlQuery)).thenReturn(to);

        assertEquals(List.of(to), logic.list(AnyTypeKind.USER.name()));
    }

    @Test
    void create() {
        LogicTestAuth.authenticate("user");

        FIQLQueryTO to = new FIQLQueryTO();
        to.setKey("key");
        FIQLQuery fiqlQuery = mock(FIQLQuery.class);

        when(binder.create(to)).thenReturn(fiqlQuery);
        when(fiqlQueryDAO.save(fiqlQuery)).thenReturn(fiqlQuery);
        when(binder.getFIQLQueryTO(fiqlQuery)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        LogicTestAuth.authenticate("user");

        FIQLQueryTO to = new FIQLQueryTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(fiqlQueryDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void deleteMissing() {
        LogicTestAuth.authenticate("user");
        doReturn(Optional.empty()).when(fiqlQueryDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        LogicTestAuth.authenticate("user");

        FIQLQuery fiqlQuery = mock(FIQLQuery.class);
        User owner = mock(User.class);
        FIQLQueryTO to = new FIQLQueryTO();
        to.setKey("key");

        when(owner.getUsername()).thenReturn("user");
        when(fiqlQuery.getOwner()).thenReturn(owner);
        doReturn(Optional.of(fiqlQuery)).when(fiqlQueryDAO).findById("key");
        when(binder.getFIQLQueryTO(fiqlQuery)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(fiqlQueryDAO).deleteById("key");
    }
}
