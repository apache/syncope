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
package org.apache.syncope.core.provisioning.java;

import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Component
public class AuditManagerImpl implements AuditManager {

    @Autowired
    private LoggerDAO loggerDAO;

    public static String getDomainAuditLoggerName(final String domain) {
        return LoggerType.AUDIT.getPrefix() + "." + domain;
    }

    public static String getDomainAuditEventLoggerName(final String domain, final String loggerName) {
        return domain + "." + loggerName;
    }

    @Override
    public boolean auditRequested(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event) {

        AuditEntry auditEntry = new AuditEntry(
                AuthContextUtils.getUsername(),
                new AuditLoggerName(type, category, subcategory, event, Result.SUCCESS),
                null,
                null,
                null);
        org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                loggerDAO.find(auditEntry.getLogger().toLoggerName());
        boolean auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        if (auditRequested) {
            return true;
        }

        auditEntry = new AuditEntry(
                AuthContextUtils.getUsername(),
                new AuditLoggerName(type, category, subcategory, event, Result.FAILURE),
                null,
                null,
                null);
        syncopeLogger = loggerDAO.find(auditEntry.getLogger().toLoggerName());
        auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        return auditRequested;
    }

    @Override
    public void audit(final AfterHandlingEvent event) {
        audit(
                event.getType(),
                event.getCategory(),
                event.getSubcategory(),
                event.getEvent(),
                event.getCondition(),
                event.getBefore(),
                event.getOutput(),
                event.getInput());
    }

    @Override
    public void audit(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result condition,
            final Object before,
            final Object output,
            final Object... input) {

        Throwable throwable = null;
        if (output instanceof Throwable) {
            throwable = (Throwable) output;
        }

        AuditEntry auditEntry = new AuditEntry(
                AuthContextUtils.getUsername(),
                new AuditLoggerName(type, category, subcategory, event, condition),
                before,
                throwable == null ? output : throwable.getMessage(),
                input);

        org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                loggerDAO.find(auditEntry.getLogger().toLoggerName());
        if (syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG) {
            Logger logger = LoggerFactory.getLogger(getDomainAuditLoggerName(AuthContextUtils.getDomain()));
            Logger eventLogger = LoggerFactory.getLogger(getDomainAuditEventLoggerName(AuthContextUtils.getDomain(),
                    syncopeLogger.getKey()));
            String serializedAuditEntry = POJOHelper.serialize(auditEntry);
            if (throwable == null) {
                logger.debug(serializedAuditEntry);
                eventLogger.debug(POJOHelper.serialize(auditEntry));
            } else {
                logger.debug(serializedAuditEntry, throwable);
                eventLogger.debug(serializedAuditEntry, throwable);
            }
        }
    }
}
