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
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecurityQuestionLogicTest {

    private SecurityQuestionDAO securityQuestionDAO;

    private UserDAO userDAO;

    private SecurityQuestionDataBinder binder;

    private SecurityQuestionLogic logic;

    @BeforeEach
    void setUp() {
        securityQuestionDAO = mock(SecurityQuestionDAO.class);
        userDAO = mock(UserDAO.class);
        binder = mock(SecurityQuestionDataBinder.class);
        logic = new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }

    @Test
    void list() {
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");

        doReturn(List.of(securityQuestion)).when(securityQuestionDAO).findAll();
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertEquals(List.of(to), logic.list());
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(securityQuestionDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void read() {
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");

        doReturn(Optional.of(securityQuestion)).when(securityQuestionDAO).findById("key");
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void create() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);

        when(binder.create(to)).thenReturn(securityQuestion);
        when(securityQuestionDAO.save(securityQuestion)).thenReturn(securityQuestion);
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(securityQuestionDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);

        doReturn(Optional.of(securityQuestion)).when(securityQuestionDAO).findById("key");
        when(securityQuestionDAO.save(securityQuestion)).thenReturn(securityQuestion);
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertSame(to, logic.update(to));
        verify(binder).update(securityQuestion, to);
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(securityQuestionDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");

        doReturn(Optional.of(securityQuestion)).when(securityQuestionDAO).findById("key");
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(securityQuestionDAO).deleteById("key");
    }

    @Test
    void readByUserWithoutUsername() {
        assertThrows(NotFoundException.class, () -> logic.readByUser(null));
    }

    @Test
    void readByUserMissing() {
        doReturn(Optional.empty()).when(userDAO).findByUsername("missing");

        assertThrows(NotFoundException.class, () -> logic.readByUser("missing"));
    }

    @Test
    void readByUserWithoutSecurityQuestion() {
        User user = mock(User.class);
        doReturn(Optional.of(user)).when(userDAO).findByUsername("user");

        assertThrows(NotFoundException.class, () -> logic.readByUser("user"));
    }

    @Test
    void readByUser() {
        User user = mock(User.class);
        SecurityQuestion securityQuestion = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");

        doReturn(Optional.of(user)).when(userDAO).findByUsername("user");
        when(user.getSecurityQuestion()).thenReturn(securityQuestion);
        when(binder.getSecurityQuestionTO(securityQuestion)).thenReturn(to);

        assertSame(to, logic.readByUser("user"));
    }
}
