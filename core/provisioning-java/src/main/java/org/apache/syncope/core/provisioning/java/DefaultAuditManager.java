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

import java.util.Date;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DefaultAuditManager implements AuditManager {

    private static final String MASKED_VALUE = "<MASKED>";

    private static Object maskSensitive(final Object object) {
        Object masked;

        if (object instanceof UserTO) {
            masked = SerializationUtils.clone((UserTO) object);
            if (((UserTO) masked).getPassword() != null) {
                ((UserTO) masked).setPassword(MASKED_VALUE);
            }
            if (((UserTO) masked).getSecurityAnswer() != null) {
                ((UserTO) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof UserPatch && ((UserPatch) object).getPassword() != null) {
            masked = SerializationUtils.clone((UserPatch) object);
            ((UserPatch) masked).getPassword().setValue(MASKED_VALUE);
        } else {
            masked = object;
        }

        return masked;
    }

    @Autowired
    private LoggerDAO loggerDAO;

    @Override
    public boolean auditRequested(
            final String who,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event) {

        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setWho(who);
        auditEntry.setLogger(new AuditLoggerName.Builder().
                type(type).category(category).subcategory(subcategory).event(event).result(Result.SUCCESS).build());
        auditEntry.setDate(new Date());

        org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                loggerDAO.find(auditEntry.getLogger().toLoggerName());
        boolean auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        if (auditRequested) {
            return true;
        }

        auditEntry.setLogger(new AuditLoggerName.Builder().
                type(type).category(category).subcategory(subcategory).event(event).result(Result.FAILURE).build());

        syncopeLogger = loggerDAO.find(auditEntry.getLogger().toLoggerName());
        auditRequested = syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG;

        return auditRequested;
    }

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

        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setWho(who);
        auditEntry.setLogger(new AuditLoggerName.Builder().
                type(type).category(category).subcategory(subcategory).event(event).result(condition).build());
        auditEntry.setDate(new Date());
        auditEntry.setBefore(POJOHelper.serialize((maskSensitive(before))));
        if (throwable == null) {
            auditEntry.setOutput(POJOHelper.serialize((maskSensitive(output))));
        } else {
            auditEntry.setOutput(throwable.getMessage());
            auditEntry.setThrowable(ExceptionUtils2.getFullStackTrace(throwable));
        }
        if (input != null) {
            for (Object obj : input) {
                auditEntry.getInputs().add(POJOHelper.serialize(obj));
            }
        }

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
