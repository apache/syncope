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
package org.apache.syncope.core.starter.actuate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EntityCacheEndpointTest {

    private EntityCacheDAO entityCacheDAO;

    private EntityCacheEndpoint endpoint;

    @BeforeEach
    void setUp() {
        entityCacheDAO = mock(EntityCacheDAO.class);
        endpoint = new EntityCacheEndpoint(entityCacheDAO);
    }

    @Test
    void statisticsDelegatesToDao() {
        Map<String, Object> statistics = Map.of("hits", 1L);
        when(entityCacheDAO.getStatistics()).thenReturn(statistics);

        assertSame(statistics, endpoint.statistics());
    }

    @Test
    void writeOperationEnablesStatistics() {
        endpoint.statistics("enable");
        verify(entityCacheDAO).enableStatistics();
    }

    @Test
    void writeOperationEnablesStatisticsIgnoringCase() {
        endpoint.statistics("ENABLE");
        verify(entityCacheDAO).enableStatistics();
    }

    @Test
    void writeOperationDisablesStatistics() {
        endpoint.statistics("disable");
        verify(entityCacheDAO).disableStatistics();
    }

    @Test
    void writeOperationResetsStatistics() {
        endpoint.statistics("reset");
        verify(entityCacheDAO).resetStatistics();
    }

    @Test
    void writeOperationResetsStatisticsIgnoringCase() {
        endpoint.statistics("RESET");
        verify(entityCacheDAO).resetStatistics();
    }

    @Test
    void writeOperationRejectsUnknownOperation() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> endpoint.statistics("unknown"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("unknown"));
    }

    @Test
    void clearCacheDelegatesToDao() {
        endpoint.clearCache();
        verify(entityCacheDAO).clearCache();
    }
}
