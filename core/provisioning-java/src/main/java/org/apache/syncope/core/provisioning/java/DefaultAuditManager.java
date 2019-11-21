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

import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.AuditEntryImpl;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultAuditManager implements AuditManager {

    @Autowired
    private LoggerDAO loggerDAO;

    @Override
    public boolean auditRequested(
            final String who,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event) {

        AuditEntry auditEntry = AuditEntryImpl.builder()
            .who(who)
            .logger(new AuditLoggerName(type, category, subcategory, event, Result.SUCCESS))
            .build();
        org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                loggerDAO.find(auditEntry.getLogger().toLoggerName());
        boolean auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        if (auditRequested) {
            return true;
        }

        auditEntry = AuditEntryImpl.builder()
            .who(who)
            .logger(new AuditLoggerName(type, category, subcategory, event, Result.FAILURE))
            .build();
        syncopeLogger = loggerDAO.find(auditEntry.getLogger().toLoggerName());
        auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        return auditRequested;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void audit(final AfterHandlingEvent event) {
        audit(
                event.getWho(),
                event.getType(),
                event.getCategory(),
                event.getSubcategory(),
                event.getEvent(),
                event.getCondition(),
                event.getBefore(),
                event.getOutput(),
                event.getInput());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void audit(
            final String who,
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

        AuditEntry auditEntry = AuditEntryImpl.builder()
            .who(who)
            .logger(new AuditLoggerName(type, category, subcategory, event, condition))
            .before(before)
            .output(throwable == null ? output : throwable.getMessage())
            .input(input)
            .build();
        
        org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                loggerDAO.find(auditEntry.getLogger().toLoggerName());
        if (syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG) {
            Logger logger = LoggerFactory.getLogger(
                    AuditLoggerName.getAuditLoggerName(AuthContextUtils.getDomain()));
            Logger eventLogger = LoggerFactory.getLogger(
                    AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), syncopeLogger.getKey()));
            String serializedAuditEntry = POJOHelper.serialize(auditEntry);

            if (throwable == null) {
                logger.debug(serializedAuditEntry);
                eventLogger.debug(serializedAuditEntry);
            } else {
                logger.debug(serializedAuditEntry, throwable);
                eventLogger.debug(serializedAuditEntry, throwable);
            }
        }
    }
}
