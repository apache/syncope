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
package org.apache.syncope.wa.starter.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.AbstractTest;
import org.apereo.cas.support.events.CasEventRepositoryFilter;
import org.apereo.cas.support.events.dao.CasEvent;
import org.junit.jupiter.api.Test;

public class WAEventRepositoryTest extends AbstractTest {

    private static AuditService AUDIT_SERVICE;

    private static WARestClient getWaRestClient() {
        AUDIT_SERVICE = mock(AuditService.class);

        WARestClient waRestClient = mock(WARestClient.class);
        when(waRestClient.isReady()).thenReturn(Boolean.TRUE);
        when(waRestClient.getService(AuditService.class)).thenReturn(AUDIT_SERVICE);

        return waRestClient;
    }

    @Test
    public void saveInternal() {
        CasEvent event = new CasEvent(1L, "Auth", "principalId", "creationTime", Map.of("timestamp", "1"));
        WAEventRepository eventRepository =
                new WAEventRepository(CasEventRepositoryFilter.noOp(), getWaRestClient());
        eventRepository.saveInternal(event);
        verify(AUDIT_SERVICE).create(any(AuditEventTO.class));
    }
}
