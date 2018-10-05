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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.syncope.common.lib.log.EventCategory;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatement;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.rest.api.LoggerWrapper;
import org.apache.syncope.common.rest.api.service.LoggerService;

public class LoggerRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public List<String> listMemoryAppenders() {
        return getService(LoggerService.class).memoryAppenders().stream().
                map(LogAppender::getName).collect(Collectors.toList());
    }

    public List<LogStatement> getLastLogStatements(final String appender, final long lastStatementTime) {
        List<LogStatement> result = new ArrayList<>();
        getService(LoggerService.class).getLastLogStatements(appender).stream().
                filter(statement -> statement.getTimeMillis() > lastStatementTime).
                forEachOrdered(statement -> {
                    result.add(statement);
                });

        return result;
    }

    public List<LoggerTO> listLogs() {
        List<LoggerTO> logs = getService(LoggerService.class).list(LoggerType.LOG);
        Collections.sort(logs, (o1, o2) -> ObjectUtils.compare(o1.getKey(), o2.getKey()));

        return logs;
    }

    public List<AuditLoggerName> listAudits() {
        return LoggerWrapper.wrap(getService(LoggerService.class).list(LoggerType.AUDIT));
    }

    public Map<String, Set<AuditLoggerName>> listAuditsByCategory() {
        Map<String, Set<AuditLoggerName>> result = new HashMap<>();
        listAudits().forEach(audit -> {
            if (!result.containsKey(audit.getCategory())) {
                result.put(audit.getCategory(), new HashSet<>());
            }

            result.get(audit.getCategory()).add(audit);
        });

        return result;
    }

    public void setLogLevel(final LoggerTO loggerTO) {
        getService(LoggerService.class).update(LoggerType.LOG, loggerTO);
    }

    public void enableAudit(final AuditLoggerName auditLoggerName) {
        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setKey(auditLoggerName.toLoggerName());
        loggerTO.setLevel(LoggerLevel.DEBUG);
        getService(LoggerService.class).update(LoggerType.AUDIT, loggerTO);
    }

    public void deleteLog(final String name) {
        getService(LoggerService.class).delete(LoggerType.LOG, name);
    }

    public void disableAudit(final AuditLoggerName auditLoggerName) {
        getService(LoggerService.class).delete(LoggerType.AUDIT, auditLoggerName.toLoggerName());
    }

    public List<EventCategory> listEvents() {
        try {
            return getService(LoggerService.class).events();
        } catch (Exception e) {
            return Collections.<EventCategory>emptyList();
        }
    }
}
