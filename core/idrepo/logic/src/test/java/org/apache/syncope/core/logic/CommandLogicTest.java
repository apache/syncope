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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CommandLogicTest {

    private ImplementationDAO implementationDAO;

    private Validator validator;

    private CommandLogic logic;

    @BeforeEach
    void setUp() {
        implementationDAO = mock(ImplementationDAO.class);
        validator = mock(Validator.class);
        logic = new CommandLogic(implementationDAO, validator);
    }

    @Test
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        doReturn(List.of()).when(implementationDAO).
                findByTypeAndKeyword(IdRepoImplementationType.COMMAND, null);

        assertEquals(0, logic.search(null, pageable).getTotalElements());
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(implementationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void runMissing() {
        CommandTO command = new CommandTO.Builder("missing").build();
        doReturn(Optional.empty()).when(implementationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.run(command));
    }

    @Test
    void resolveReference() {
        assertThrows(UnsupportedOperationException.class, () -> logic.resolveBeanReference(
                CommandLogic.class.getMethod("run", CommandTO.class), new CommandTO.Builder("key").build()));
    }
}
