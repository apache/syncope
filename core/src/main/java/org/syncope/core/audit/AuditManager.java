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
package org.syncope.core.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.syncope.types.AuditElements;
import org.syncope.types.SyncopeLoggerType;

public class AuditManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AuditManager.class);

    private String getLoggerName(final AuditElements.Category category, final Enum<?> subcategory,
            final AuditElements.Result result) {

        StringBuilder loggerName = new StringBuilder();
        loggerName.append(SyncopeLoggerType.AUDIT.getPrefix()).append('.').append(category.name()).append('.').append(
                subcategory.name()).append('.').append(result.name());
        return loggerName.toString();
    }

    public void audit(final AuditElements.Category category, final Enum<?> subcategory,
            final AuditElements.Result result, final String message) {

        audit(category, subcategory, result, message, null);
    }

    public void audit(final AuditElements.Category category, final Enum<?> subcategory,
            final AuditElements.Result result, final String message, final Throwable t) {

        if (category == null || subcategory == null || result == null) {
            LOG.error("Invalid request: some null items {} {} {}", new Object[] { category, subcategory, result });
        } else if (!AuditElements.getSubCategories(category).contains(subcategory)) {
            LOG.error("Invalid request: {} does not belong to {}", new Object[] { subcategory, category });
        } else {
            StringBuilder auditMessage = new StringBuilder();

            final SecurityContext ctx = SecurityContextHolder.getContext();
            if (ctx != null && ctx.getAuthentication() != null) {
                auditMessage.append('[').append(ctx.getAuthentication().getName()).append(']').append(' ');
            }
            auditMessage.append(message);

            Logger logger = LoggerFactory.getLogger(getLoggerName(category, subcategory, result));
            if (t == null) {
                logger.debug(auditMessage.toString());
            } else {
                logger.debug(auditMessage.toString(), t);
            }
        }
    }
}
