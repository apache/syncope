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
package org.apache.syncope.wa.starter.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.AbstractTest;
import org.apereo.inspektr.audit.AuditActionContext;
import org.junit.jupiter.api.Test;

public class WAAuditTrailManagerTest extends AbstractTest {

    private static AuditService LOGGER_SERVICE;

    private static WARestClient getWaRestClient() {
        WARestClient restClient = mock(WARestClient.class);
        SyncopeClient syncopeClient = mock(SyncopeClient.class);
        LOGGER_SERVICE = mock(AuditService.class);

        when(restClient.getSyncopeClient()).thenReturn(syncopeClient);
        when(syncopeClient.getService(AuditService.class)).thenReturn(LOGGER_SERVICE);

        return restClient;
    }

    @Test
    public void saveAuditRecord() {
        AuditActionContext audit = new AuditActionContext("principal", "resourceOperatedUpon", "actionPerformed",
                "applicationCode", new Date(), "clientIpAddress", "serverIpAddress", "userAgent");
        WAAuditTrailManager auditTrailManager = new WAAuditTrailManager(getWaRestClient());
        auditTrailManager.saveAuditRecord(audit);
        verify(LOGGER_SERVICE).create(any(AuditEntry.class));
    }
}
