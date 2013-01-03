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
package org.apache.syncope.services;

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.to.LoggerTO;
import org.apache.syncope.types.AuditLoggerName;
import org.springframework.web.client.RestTemplate;

import ch.qos.logback.classic.Level;

public class LoggerServiceProxy extends SpringServiceProxy implements LoggerService {

    public LoggerServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public List<LoggerTO> listLogs() {
        return Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/log/list", LoggerTO[].class));
    }

    @Override
    public List<AuditLoggerName> listAudits() {
        return Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/audit/list",
                AuditLoggerName[].class));
    }

    @Override
    public LoggerTO setLogLevel(String name, Level level) {
        return restTemplate.postForObject(BASE_URL + "logger/log/{name}/{level}", null, LoggerTO.class, name,
                level);
    }

    @Override
    public LoggerTO deleteLog(String name) {
        return restTemplate.getForObject(BASE_URL + "logger/log/delete/{name}", LoggerTO.class, name);
    }

    @Override
    public void enableAudit(AuditLoggerName auditLoggerName) {
        restTemplate.put(BASE_URL + "logger/audit/enable", auditLoggerName);
    }

    @Override
    public void disableAudit(AuditLoggerName auditLoggerName) {
        restTemplate.put(BASE_URL + "logger/audit/disable", auditLoggerName);
    }

}
