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
package org.apache.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.apache.syncope.client.to.LoggerTO;
import org.apache.syncope.types.AuditElements;
import org.apache.syncope.types.AuditLoggerName;
import org.apache.syncope.types.SyncopeLoggerLevel;

public class LoggerTestITCase extends AbstractTest {

    @Test
    public void listLogs() {
        List<LoggerTO> loggers =
                Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/log/list", LoggerTO[].class));
        assertNotNull(loggers);
        assertFalse(loggers.isEmpty());
        for (LoggerTO logger : loggers) {
            assertNotNull(logger);
        }
    }

    @Test
    public void listAudits() {
        List<AuditLoggerName> audits =
                Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/audit/list", AuditLoggerName[].class));
        assertNotNull(audits);
        assertFalse(audits.isEmpty());
        for (AuditLoggerName audit : audits) {
            assertNotNull(audit);
        }
    }

    @Test
    public void setLevel() {
        List<LoggerTO> loggers =
                Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/log/list", LoggerTO[].class));
        assertNotNull(loggers);
        int startSize = loggers.size();

        LoggerTO logger = restTemplate.postForObject(BASE_URL + "logger/log/{name}/{level}", null, LoggerTO.class,
                "TEST", "INFO");
        assertNotNull(logger);
        assertEquals(SyncopeLoggerLevel.INFO, logger.getLevel());

        loggers = Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/log/list", LoggerTO[].class));
        assertNotNull(loggers);
        assertEquals(startSize + 1, loggers.size());
    }

    @Test
    public void enableDisableAudit() {
        AuditLoggerName auditLoggerName = new AuditLoggerName(AuditElements.Category.report,
                AuditElements.ReportSubCategory.listExecutions, AuditElements.Result.failure);

        List<AuditLoggerName> audits =
                Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/audit/list", AuditLoggerName[].class));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));

        restTemplate.put(BASE_URL + "logger/audit/enable", auditLoggerName);

        audits = Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/audit/list", AuditLoggerName[].class));
        assertNotNull(audits);
        assertTrue(audits.contains(auditLoggerName));

        restTemplate.put(BASE_URL + "logger/audit/disable", auditLoggerName);

        audits = Arrays.asList(restTemplate.getForObject(BASE_URL + "logger/audit/list", AuditLoggerName[].class));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));
    }
}
