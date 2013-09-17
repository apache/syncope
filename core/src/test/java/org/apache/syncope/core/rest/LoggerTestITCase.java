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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.List;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.util.CollectionWrapper;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class LoggerTestITCase extends AbstractTest {

    @Test
    public void listLogs() {
        List<LoggerTO> loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertFalse(loggers.isEmpty());
        for (LoggerTO logger : loggers) {
            assertNotNull(logger);
        }
    }

    @Test
    public void listAudits() throws ParseException {
        List<LoggerTO> audits = loggerService.list(LoggerType.AUDIT);

        assertNotNull(audits);
        assertFalse(audits.isEmpty());
        for (LoggerTO audit : audits) {
            assertNotNull(AuditLoggerName.fromLoggerName(audit.getName()));
        }
    }

    @Test
    public void setLevel() {
        List<LoggerTO> loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        int startSize = loggers.size();

        LoggerTO logger = new LoggerTO();
        logger.setName("TEST");
        logger.setLevel(LoggerLevel.INFO);
        loggerService.update(LoggerType.LOG, logger.getName(), logger);
        logger = loggerService.read(LoggerType.LOG, logger.getName());
        assertNotNull(logger);
        assertEquals(LoggerLevel.INFO, logger.getLevel());

        loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertEquals(startSize + 1, loggers.size());

        // TEST Delete
        loggerService.delete(LoggerType.LOG, "TEST");
        loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        assertEquals(startSize, loggers.size());
    }

    @Test
    public void enableDisableAudit() {
        AuditLoggerName auditLoggerName = new AuditLoggerName(AuditElements.Category.report,
                AuditElements.ReportSubCategory.deleteExecution, AuditElements.Result.failure);

        List<AuditLoggerName> audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));

        LoggerTO loggerTO = new LoggerTO();
        String name = auditLoggerName.toLoggerName();
        loggerTO.setName(name);
        loggerTO.setLevel(LoggerLevel.DEBUG);
        loggerService.update(LoggerType.AUDIT, name, loggerTO);

        audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertTrue(audits.contains(auditLoggerName));

        loggerService.delete(LoggerType.AUDIT, auditLoggerName.toLoggerName());

        audits = CollectionWrapper.wrapLogger(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));
    }
}
