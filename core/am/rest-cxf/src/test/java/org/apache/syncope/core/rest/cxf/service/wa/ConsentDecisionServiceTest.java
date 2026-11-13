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
package org.apache.syncope.core.rest.cxf.service.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.core.logic.wa.ConsentDecisionLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentDecisionServiceTest {

    private ConsentDecisionLogic logic;

    private ConsentDecisionServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(ConsentDecisionLogic.class);
        service = new ConsentDecisionServiceImpl(logic);
    }

    @Test
    void deleteByOwnerAndId() {
        service.delete("owner", 1L);
        verify(logic).delete("owner", 1L);
    }

    @Test
    void deleteByOwner() {
        service.delete("owner");
        verify(logic).delete("owner");
    }

    @Test
    void deleteAll() {
        service.deleteAll();
        verify(logic).deleteAll();
    }

    @Test
    void store() {
        WAConsentDecision decision = new WAConsentDecision();
        service.store("owner", decision);
        verify(logic).store("owner", decision);
    }

    @Test
    void readByOwnerAndService() {
        WAConsentDecision decision = new WAConsentDecision();
        when(logic.read("owner", "service")).thenReturn(decision);
        assertSame(decision, service.read("owner", "service"));
    }

    @Test
    void readByOwner() {
        WAConsentDecision decision = new WAConsentDecision();
        when(logic.read("owner")).thenReturn(List.of(decision));
        PagedResult<WAConsentDecision> result = service.read("owner");
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(decision), result.getResult());
    }

    @Test
    void list() {
        WAConsentDecision decision = new WAConsentDecision();
        when(logic.list()).thenReturn(List.of(decision));
        PagedResult<WAConsentDecision> result = service.list();
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(decision), result.getResult());
    }
}
