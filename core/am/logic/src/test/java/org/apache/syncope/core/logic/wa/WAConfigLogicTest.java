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
package org.apache.syncope.core.logic.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WAConfigLogicTest {

    private org.apache.syncope.common.keymaster.client.api.ServiceOps serviceOps;

    private WAConfigDataBinder binder;

    private WAConfigDAO waConfigDAO;

    private org.apache.syncope.core.spring.security.SecurityProperties securityProperties;

    private WAConfigLogic logic;

    @BeforeEach
    void setUp() {
        serviceOps = mock(org.apache.syncope.common.keymaster.client.api.ServiceOps.class);
        binder = mock(WAConfigDataBinder.class);
        waConfigDAO = mock(WAConfigDAO.class);
        securityProperties = mock(org.apache.syncope.core.spring.security.SecurityProperties.class);

        logic = new WAConfigLogic(serviceOps, binder, waConfigDAO, securityProperties);
    }

    @AfterEach
    void tearDown() {
        org.apache.syncope.core.logic.LogicTestAuth.clear();
    }

    @Test
    void list() {
        doReturn(List.of()).when(waConfigDAO).findAll();

        assertEquals(List.of(), logic.list());
    }

    @Test
    void getMissing() {
        doReturn(Optional.empty()).when(waConfigDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.get("missing"));
    }
}
