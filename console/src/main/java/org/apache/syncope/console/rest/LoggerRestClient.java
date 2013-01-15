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

import org.apache.syncope.client.to.LoggerTO;
import org.apache.syncope.services.LoggerService;
import org.apache.syncope.types.AuditElements;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditLoggerName;
import org.apache.syncope.types.SyncopeLoggerLevel;
import org.springframework.stereotype.Component;

@Component
public class LoggerRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public List<LoggerTO> listLogs() {
        return getService(LoggerService.class).listLogs();
    }

    public List<AuditLoggerName> listAudits() {
        return getService(LoggerService.class).listAudits();
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

    public void setLogLevel(final String name, final SyncopeLoggerLevel level) {
        getService(LoggerService.class).update(name, level.getLevel());
    }

    public void enableAudit(final AuditLoggerName auditLoggerName) {
        getService(LoggerService.class).enableAudit(auditLoggerName);
    }

    public LoggerTO deleteLog(final String name) {
        return getService(LoggerService.class).delete(name);
    }

    public void disableAudit(final AuditLoggerName auditLoggerName) {
        getService(LoggerService.class).disableAudit(auditLoggerName);
    }
}
