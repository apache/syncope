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
package org.apache.syncope.core.persistence.api.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.springframework.data.domain.Pageable;

public interface AuditEventDAO {

    AuditEvent save(AuditEvent auditEvent);

    long count(
            String entityKey,
            OpEvent.CategoryType type,
            String category,
            String subcategory,
            String op,
            OpEvent.Outcome outcome,
            OffsetDateTime before,
            OffsetDateTime after);

    default AuditEventTO toAuditEventTO(final AuditEvent auditEvent) {
        AuditEventTO auditEventTO = new AuditEventTO();
        auditEventTO.setKey(auditEvent.getKey());
        auditEventTO.setOpEvent(OpEvent.fromString(auditEvent.getOpEvent()));
        auditEventTO.setWho(auditEvent.getWho());
        auditEventTO.setWhen(auditEvent.getWhen());
        auditEventTO.setBefore(auditEvent.getBefore());
        auditEventTO.getInputs().addAll(auditEvent.getInputs());
        auditEventTO.setOutput(auditEvent.getOutput());
        auditEventTO.setThrowable(auditEvent.getThrowable());
        return auditEventTO;
    }

    List<AuditEventTO> search(
            String entityKey,
            OpEvent.CategoryType type,
            String category,
            String subcategory,
            String op,
            OpEvent.Outcome outcome,
            OffsetDateTime before,
            OffsetDateTime after,
            Pageable pageable);
}
