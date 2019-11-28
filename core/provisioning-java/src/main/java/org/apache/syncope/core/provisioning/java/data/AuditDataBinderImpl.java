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
package org.apache.syncope.core.provisioning.java.data;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.springframework.stereotype.Component;

@Component
public class AuditDataBinderImpl implements AuditDataBinder {

    @Override
    public AuditEntryTO getAuditTO(final AuditEntry auditEntry) {
        AuditEntryTO auditTO = new AuditEntryTO();
        auditTO.setKey(auditEntry.getKey());
        auditTO.setWho(auditEntry.getWho());
        auditTO.setDate(auditEntry.getDate());
        auditTO.setThrowable(auditEntry.getThrowable());
        auditTO.setLoggerName(auditEntry.getLogger().toLoggerName());

        auditTO.setSubCategory(auditEntry.getLogger().getSubcategory());
        auditTO.setEvent(auditEntry.getLogger().getEvent());

        if (auditEntry.getLogger().getResult() != null) {
            auditTO.setResult(auditEntry.getLogger().getResult().name());
        }

        if (auditEntry.getBefore() != null) {
            auditTO.setBefore(ToStringBuilder.reflectionToString(auditEntry.getBefore(), ToStringStyle.JSON_STYLE));
        }

        if (auditEntry.getInput() != null) {
            auditTO.getInputs().addAll(Arrays.stream(auditEntry.getInput())
                .map(input -> input == null
                    ? "null"
                    : ToStringBuilder.reflectionToString(input, ToStringStyle.JSON_STYLE))
                .collect(Collectors.toList()));
        }

        if (auditEntry.getOutput() != null) {
            auditTO.setOutput(ToStringBuilder.reflectionToString(auditEntry.getOutput(), ToStringStyle.JSON_STYLE));
        }

        return auditTO;
    }

    @Override
    public AuditEntryTO returnAuditTO(final AuditEntryTO auditEntryTO) {
        return auditEntryTO;
    }
}
