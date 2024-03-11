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
package org.apache.syncope.fit.core.reference;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.provisioning.api.AuditEventProcessor;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileAuditProcessor implements AuditEventProcessor {

    protected static final Logger LOG = LoggerFactory.getLogger(TestFileAuditProcessor.class);

    private static final Set<OpEvent> OP_EVENTS = Set.of(
            new OpEvent(
                    OpEvent.CategoryType.LOGIC,
                    ResourceLogic.class.getSimpleName(),
                    null,
                    "create",
                    OpEvent.Outcome.SUCCESS),
            new OpEvent(
                    OpEvent.CategoryType.LOGIC,
                    ConnectorLogic.class.getSimpleName(),
                    null,
                    "update",
                    OpEvent.Outcome.SUCCESS));

    private final AuditEventDAO auditEventDAO;

    public TestFileAuditProcessor(final AuditEventDAO auditEventDAO) {
        this.auditEventDAO = auditEventDAO;
    }

    @Override
    public Set<OpEvent> getEvents(final String domain) {
        return OP_EVENTS;
    }

    @Override
    public void process(final String domain, final AuditEvent event) {
        String fileName = System.getProperty("syncope.log.dir") + "/audit_for_" + domain + "_file";
        String content = POJOHelper.serialize(auditEventDAO.toAuditEventTO(event)) + '\n';
        try {
            Files.write(
                    Paths.get(fileName),
                    content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            LOG.error("Could not append audit event {} to file {}", event, fileName, e);
        }
    }
}
