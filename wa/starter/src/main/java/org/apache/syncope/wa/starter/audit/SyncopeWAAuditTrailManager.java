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
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.apereo.cas.audit.spi.AbstractAuditTrailManager;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.inspektr.audit.AuditActionContext;

public class SyncopeWAAuditTrailManager extends AbstractAuditTrailManager {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final WARestClient waRestClient;

    public SyncopeWAAuditTrailManager(final WARestClient restClient) {
        super(true);
        this.waRestClient = restClient;
    }

    @Override
    protected void saveAuditRecord(final AuditActionContext audit) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.debug("Syncope client is not yet ready to store audit record");
            return;
        }

        LOG.info("Loading application definitions");
        try {
            String output = MAPPER.writeValueAsString(Map.of("resource", audit.getResourceOperatedUpon(),
                    "clientIpAddress", audit.getClientIpAddress(),
                    "serverIpAddress", audit.getServerIpAddress()));

            AuditEntry auditEntry = new AuditEntry();
            auditEntry.setWho(audit.getPrincipal());
            auditEntry.setDate(
                    audit.getWhenActionWasPerformed().toInstant().atOffset(OffsetDateTime.now().getOffset()));
            auditEntry.setOutput(output);
            AuditElements.Result result = StringUtils.containsIgnoreCase(audit.getActionPerformed(), "fail")
                    ? AuditElements.Result.FAILURE
                    : AuditElements.Result.SUCCESS;

            AuditLoggerName auditLogger = new AuditLoggerName(
                    AuditElements.EventCategoryType.WA,
                    null,
                    AuditElements.AUTHENTICATION_CATEGORY.toUpperCase(),
                    audit.getActionPerformed(),
                    result);
            auditEntry.setLogger(auditLogger);
            syncopeClient.getService(AuditService.class).create(auditEntry);
        } catch (JsonProcessingException e) {
            LOG.error("During serialization", e);
        }
    }

    @Override
    public Set<? extends AuditActionContext> getAuditRecordsSince(final LocalDate sinceDate) {
        throw new UnsupportedOperationException("Fetching audit events from WA is not supported");
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException("Removing audit events from WA is not supported");
    }
}
