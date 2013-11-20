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
package org.apache.syncope.core.audit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.core.persistence.beans.SyncopeLogger;
import org.apache.syncope.core.persistence.dao.LoggerDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AuditManager.class);

    @Autowired
    private LoggerDAO loggerDAO;

    public void audit(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final Object... input) {

        final Throwable throwable;
        final StringBuilder message = new StringBuilder();

        message.append("BEFORE:\n");
        message.append("\t").append(before == null ? "unknown" : before).append("\n");

        message.append("INPUT:\n");

        if (ArrayUtils.isNotEmpty(input)) {
            for (Object obj : input) {
                message.append("\t").append(obj == null ? null : obj.toString()).append("\n");
            }
        } else {
            message.append("\t").append("none").append("\n");
        }

        message.append("OUTPUT:\n");

        if (output instanceof Throwable) {
            throwable = (Throwable) output;
            message.append("\t").append(throwable.getMessage());
        } else {
            throwable = null;
            message.append("\t").append(output == null ? "none" : output.toString());
        }

        AuditLoggerName auditLoggerName = null;
        try {
            auditLoggerName = new AuditLoggerName(type, category, subcategory, event, result);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid audit parameters, aborting...", e);
        }

        if (auditLoggerName != null) {
            SyncopeLogger syncopeLogger = loggerDAO.find(auditLoggerName.toLoggerName());
            if (syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG) {
                StringBuilder auditMessage = new StringBuilder();

                final SecurityContext ctx = SecurityContextHolder.getContext();
                if (ctx != null && ctx.getAuthentication() != null) {
                    auditMessage.append('[').append(ctx.getAuthentication().getName()).append(']').append(' ');
                }
                auditMessage.append(message);

                final Logger logger = LoggerFactory.getLogger(auditLoggerName.toLoggerName());
                if (throwable == null) {
                    logger.debug(auditMessage.toString());
                } else {
                    logger.debug(auditMessage.toString(), throwable);
                }
            }
        }
    }
}
