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
package org.apache.syncope.core.misc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditManager {

    private static final Logger LOG = LoggerFactory.getLogger(AuditManager.class);

    @Autowired
    private LoggerDAO loggerDAO;

    public static String getDomainAuditLoggerName(final String domain) {
        return LoggerType.AUDIT.getPrefix() + "." + domain;
    }

    @Transactional(readOnly = true)
    public void audit(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final Object... input) {

        StringBuilder message = new StringBuilder(32);

        message.append("BEFORE:\n").
                append('\t').append(before == null ? "unknown" : before).append('\n');

        message.append("INPUT:\n");

        if (ArrayUtils.isNotEmpty(input)) {
            for (Object obj : input) {
                message.append('\t').append(obj == null ? null : obj.toString()).append('\n');
            }
        } else {
            message.append('\t').append("none").append('\n');
        }

        message.append("OUTPUT:\n");

        Throwable throwable;
        if (output instanceof Throwable) {
            throwable = (Throwable) output;
            message.append('\t').append(throwable.getMessage());
        } else {
            throwable = null;
            message.append('\t').append(output == null ? "none" : output.toString());
        }

        AuditLoggerName auditLoggerName = null;
        try {
            auditLoggerName = new AuditLoggerName(type, category, subcategory, event, result);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid audit parameters, aborting...", e);
        }

        if (auditLoggerName != null) {
            org.apache.syncope.core.persistence.api.entity.Logger syncopeLogger =
                    loggerDAO.find(auditLoggerName.toLoggerName());
            if (syncopeLogger != null && syncopeLogger.getLevel() == LoggerLevel.DEBUG) {
                StringBuilder auditMessage = new StringBuilder();

                SecurityContext ctx = SecurityContextHolder.getContext();
                if (ctx != null && ctx.getAuthentication() != null) {
                    auditMessage.append('[').append(ctx.getAuthentication().getName()).append("] ");
                }
                auditMessage.append(message);

                String domain = AuthContextUtils.getDomain();
                if (input != null && input.length > 0 && input[0] instanceof UsernamePasswordAuthenticationToken) {
                    UsernamePasswordAuthenticationToken token =
                            UsernamePasswordAuthenticationToken.class.cast(input[0]);
                    if (token.getDetails() instanceof SyncopeAuthenticationDetails) {
                        domain = SyncopeAuthenticationDetails.class.cast(token.getDetails()).getDomain();
                    }
                }

                Logger logger = LoggerFactory.getLogger(getDomainAuditLoggerName(domain));
                if (throwable == null) {
                    logger.debug(auditMessage.toString());
                } else {
                    logger.debug(auditMessage.toString(), throwable);
                }
            }
        }
    }
}
