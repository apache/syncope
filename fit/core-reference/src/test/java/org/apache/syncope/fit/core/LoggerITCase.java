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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.rest.api.LoggerWrapper;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class LoggerITCase extends AbstractITCase {

    @Test
    public void listMemoryAppenders() {
        List<LogAppender> memoryAppenders = loggerService.memoryAppenders();
        assertNotNull(memoryAppenders);
        assertFalse(memoryAppenders.isEmpty());
        for (LogAppender appender : memoryAppenders) {
            assertNotNull(appender);
            assertNotNull(appender.getName());
        }
    }

    @Test
    public void lastStatements() {
        List<LogStatementTO> statements = loggerService.getLastLogStatements("connid");
        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        LogStatementTO statement = statements.get(0);
        assertNotNull(statement);
        assertNotNull(statement.getLoggerName());
        assertNotNull(statement.getLevel());
        assertNotNull(statement.getMessage());
        assertNotNull(statement.getTimeMillis());
    }

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
            assertNotNull(AuditLoggerName.fromLoggerName(audit.getKey()));
        }
    }

    @Test
    public void setLevel() {
        List<LoggerTO> loggers = loggerService.list(LoggerType.LOG);
        assertNotNull(loggers);
        int startSize = loggers.size();

        LoggerTO logger = new LoggerTO();
        logger.setKey("TEST");
        logger.setLevel(LoggerLevel.INFO);
        loggerService.update(LoggerType.LOG, logger);
        logger = loggerService.read(LoggerType.LOG, logger.getKey());
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
        AuditLoggerName auditLoggerName = new AuditLoggerName(
                EventCategoryType.LOGIC,
                ReportLogic.class.getSimpleName(),
                null,
                "deleteExecution",
                AuditElements.Result.FAILURE);

        List<AuditLoggerName> audits = LoggerWrapper.wrap(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));

        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setKey(auditLoggerName.toLoggerName());
        loggerTO.setLevel(LoggerLevel.DEBUG);
        loggerService.update(LoggerType.AUDIT, loggerTO);

        audits = LoggerWrapper.wrap(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertTrue(audits.contains(auditLoggerName));

        loggerService.delete(LoggerType.AUDIT, auditLoggerName.toLoggerName());

        audits = LoggerWrapper.wrap(loggerService.list(LoggerType.AUDIT));
        assertNotNull(audits);
        assertFalse(audits.contains(auditLoggerName));
    }

    @Test
    public void listAuditEvents() {
        final List<EventCategoryTO> events = loggerService.events();

        boolean found = false;

        for (EventCategoryTO eventCategoryTO : events) {
            if (UserLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("list"));
                assertFalse(eventCategoryTO.getEvents().contains("doCreate"));
                assertFalse(eventCategoryTO.getEvents().contains("setStatusOnWfAdapter"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (GroupLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("list"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (ResourceLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("read"));
                assertTrue(eventCategoryTO.getEvents().contains("delete"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (AnyTypeKind.USER.name().toLowerCase().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_LDAP.equals(eventCategoryTO.getSubcategory())
                        && EventCategoryType.PULL == eventCategoryTO.getType()) {
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.CREATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.UPDATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (AnyTypeKind.USER.name().toLowerCase().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_CSV.equals(eventCategoryTO.getSubcategory())
                        && EventCategoryType.PROPAGATION == eventCategoryTO.getType()) {
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.CREATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.UPDATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (EventCategoryType.TASK == eventCategoryTO.getType()
                    && "TestSampleJobDelegate".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategoryTO eventCategoryTO : events) {
            if (EventCategoryType.TASK == eventCategoryTO.getType()
                    && "PullJobDelegate".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void issueSYNCOPE708() {
        try {
            loggerService.read(LoggerType.LOG, "notExists");
            fail("Reading non-existing logger, it should go in exception");
        } catch (final WebServiceException ex) {
            fail("Exception is WebServiceException but it should be SyncopeClientException");
        } catch (final SyncopeClientException ex) {
            assertEquals(Response.Status.NOT_FOUND, ex.getType().getResponseStatus());
        }
    }

    @Test
    public void issueSYNCOPE976() {
        List<EventCategoryTO> events = loggerService.events();
        assertNotNull(events);

        EventCategoryTO userLogic = IterableUtils.find(events, new Predicate<EventCategoryTO>() {

            @Override
            public boolean evaluate(final EventCategoryTO object) {
                return "UserLogic".equals(object.getCategory());
            }
        });
        assertNotNull(userLogic);
        assertEquals(1, IterableUtils.frequency(userLogic.getEvents(), "create"));
    }
}
