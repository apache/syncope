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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.audit.spi.AbstractAuditTrailManager;
import org.apereo.inspektr.audit.AuditActionContext;

public class WAAuditTrailManager extends AbstractAuditTrailManager {

    private final WARestClient waRestClient;

    public WAAuditTrailManager(final WARestClient restClient) {
        super(true);
        this.waRestClient = restClient;
    }

    @Override
    protected void saveAuditRecord(final AuditActionContext audit) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to store audit record");
            return;
        }

        LOG.debug("Saving audit record {}", audit);
        try {
            String output = MAPPER.writeValueAsString(Map.of(
                    "resource", audit.getResourceOperatedUpon(),
                    "clientIpAddress", audit.getClientInfo().getClientIpAddress(),
                    "serverIpAddress", audit.getClientInfo().getServerIpAddress()));

            AuditEventTO auditEvent = new AuditEventTO();
            auditEvent.setWho(audit.getPrincipal());
            auditEvent.setWhen(audit.getWhenActionWasPerformed().atOffset(OffsetDateTime.now().getOffset()));
            auditEvent.setOutput(output);
            OpEvent.Outcome result = StringUtils.containsIgnoreCase(audit.getActionPerformed(), "fail")
                    ? OpEvent.Outcome.FAILURE
                    : OpEvent.Outcome.SUCCESS;

            OpEvent opEvent = new OpEvent(
                    OpEvent.CategoryType.WA,
                    null,
                    OpEvent.AUTHENTICATION_CATEGORY.toUpperCase(),
                    audit.getActionPerformed(),
                    result);
            auditEvent.setOpEvent(opEvent);
            waRestClient.getService(AuditService.class).create(auditEvent);
        } catch (JsonProcessingException e) {
            LOG.error("During serialization", e);
        }
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException("Removing audit events from WA is not supported");
    }
}
