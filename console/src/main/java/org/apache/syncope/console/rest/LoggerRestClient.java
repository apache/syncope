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
package org.apache.syncope.console.rest;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.stereotype.Component;

@Component
public class LoggerRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public List<LoggerTO> listLogs() {
        return getService(LoggerService.class).list(LoggerType.LOG);
    }

    public List<AuditLoggerName> listAudits() {
        List<LoggerTO> logger = getService(LoggerService.class).list(LoggerType.AUDIT);

        return CollectionWrapper.wrapLogger(logger);
    }

    public Map<AuditElements.Category, Set<AuditLoggerName>> listAuditsByCategory() {
        Map<Category, Set<AuditLoggerName>> result = new EnumMap<Category, Set<AuditLoggerName>>(Category.class);
        for (AuditLoggerName auditLoggerName : listAudits()) {
            if (!result.containsKey(auditLoggerName.getCategory())) {
                result.put(auditLoggerName.getCategory(), new HashSet<AuditLoggerName>());
            }

            result.get(auditLoggerName.getCategory()).add(auditLoggerName);
        }

        return result;
    }

    public void setLogLevel(final String name, final LoggerLevel level) {
        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setName(name);
        loggerTO.setLevel(level);
        getService(LoggerService.class).update(LoggerType.LOG, name, loggerTO);
    }

    public void enableAudit(final AuditLoggerName auditLoggerName) {
        String name = auditLoggerName.toLoggerName();
        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setName(name);
        loggerTO.setLevel(LoggerLevel.DEBUG);
        getService(LoggerService.class).update(LoggerType.AUDIT, name, loggerTO);
    }

    public void deleteLog(final String name) {
        getService(LoggerService.class).delete(LoggerType.LOG, name);
    }

    public void disableAudit(final AuditLoggerName auditLoggerName) {
        getService(LoggerService.class).delete(LoggerType.AUDIT, auditLoggerName.toLoggerName());
    }
}
