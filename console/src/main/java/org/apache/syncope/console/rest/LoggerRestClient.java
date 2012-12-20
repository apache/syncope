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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.apache.syncope.client.to.LoggerTO;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.types.AuditElements;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditLoggerName;
import org.apache.syncope.types.SyncopeLoggerLevel;

@Component
public class LoggerRestClient extends BaseRestClient {

    public List<LoggerTO> listLogs() {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "logger/log/list", LoggerTO[].class));
    }

    public List<AuditLoggerName> listAudits() {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "logger/audit/list", AuditLoggerName[].class));
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
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "logger/log/{name}/{level}", null, LoggerTO.class, name, level);
    }

    public void enableAudit(final AuditLoggerName auditLoggerName) {
        SyncopeSession.get().getRestTemplate().put(
                baseURL + "logger/audit/enable", auditLoggerName);
    }

    public LoggerTO deleteLog(final String name) {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "logger/log/delete/{name}", LoggerTO.class, name);
    }

    public void disableAudit(final AuditLoggerName auditLoggerName) {
        SyncopeSession.get().getRestTemplate().put(
                baseURL + "logger/audit/disable", auditLoggerName);
    }
}
