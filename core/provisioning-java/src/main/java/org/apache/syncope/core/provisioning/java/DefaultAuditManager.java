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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

@Transactional(readOnly = true)
public class DefaultAuditManager implements AuditManager {

    protected static final String MASKED_VALUE = "<MASKED>";

    protected static Object maskSensitive(final Object object) {
        Object masked;

        if (object instanceof UserTO) {
            masked = SerializationUtils.clone((UserTO) object);
            if (((UserTO) masked).getPassword() != null) {
                ((UserTO) masked).setPassword(MASKED_VALUE);
            }
            if (((UserTO) masked).getSecurityAnswer() != null) {
                ((UserTO) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof UserCR) {
            masked = SerializationUtils.clone((UserCR) object);
            if (((UserCR) masked).getPassword() != null) {
                ((UserCR) masked).setPassword(MASKED_VALUE);
            }
            if (((UserCR) masked).getSecurityAnswer() != null) {
                ((UserCR) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof UserUR && ((UserUR) object).getPassword() != null) {
            masked = SerializationUtils.clone((UserUR) object);
            ((UserUR) masked).getPassword().setValue(MASKED_VALUE);
        } else {
            masked = object;
        }

        return masked;
    }

    protected final AuditConfDAO auditConfDAO;

    public DefaultAuditManager(final AuditConfDAO auditConfDAO) {
        this.auditConfDAO = auditConfDAO;
    }

    @Override
    public boolean auditRequested(
            final String who,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event) {

        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setWho(who);
        auditEntry.setLogger(new AuditLoggerName(type, category, subcategory, event, Result.SUCCESS));
        auditEntry.setDate(OffsetDateTime.now());

        AuditConf auditConf = auditConfDAO.find(auditEntry.getLogger().toAuditKey());
        boolean auditRequested = auditConf != null && auditConf.isActive();

        if (auditRequested) {
            return true;
        }

        auditEntry.setLogger(new AuditLoggerName(type, category, subcategory, event, Result.FAILURE));

        auditConf = auditConfDAO.find(auditEntry.getLogger().toAuditKey());
        auditRequested = auditConf != null && auditConf.isActive();

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

        AuditLoggerName auditLoggerName = new AuditLoggerName(type, category, subcategory, event, condition);

        AuditConf audit = auditConfDAO.find(auditLoggerName.toAuditKey());
        if (audit != null && audit.isActive()) {
            Throwable throwable = null;
            if (output instanceof Throwable) {
                throwable = (Throwable) output;
            }

            AuditEntry auditEntry = new AuditEntry();
            auditEntry.setWho(who);
            auditEntry.setLogger(auditLoggerName);
            auditEntry.setDate(OffsetDateTime.now());
            auditEntry.setBefore(POJOHelper.serialize((maskSensitive(before))));
            if (throwable == null) {
                auditEntry.setOutput(POJOHelper.serialize((maskSensitive(output))));
            } else {
                auditEntry.setOutput(throwable.getMessage());
                auditEntry.setThrowable(ExceptionUtils2.getFullStackTrace(throwable));
            }
            if (input != null) {
                auditEntry.getInputs().addAll(Arrays.stream(input).
                        map(DefaultAuditManager::maskSensitive).map(POJOHelper::serialize).
                        collect(Collectors.toList()));
            }

            Logger logger = LoggerFactory.getLogger(
                    AuditLoggerName.getAuditLoggerName(AuthContextUtils.getDomain()));
            Logger eventLogger = LoggerFactory.getLogger(
                    AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), audit.getKey()));
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
