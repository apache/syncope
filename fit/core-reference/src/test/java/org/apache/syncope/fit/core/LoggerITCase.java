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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.EventCategory;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatement;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.LoggerWrapper;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoggerITCase extends AbstractITCase {

    @Test
    public void listMemoryAppenders() {
        List<LogAppender> memoryAppenders = loggerService.memoryAppenders();
        assertNotNull(memoryAppenders);
        assertFalse(memoryAppenders.isEmpty());
        memoryAppenders.forEach(appender -> {
            assertNotNull(appender);
            assertNotNull(appender.getName());
        });
    }

    @Test
    public void lastStatements() {
        List<LogStatement> statements = loggerService.getLastLogStatements("connid");
        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        LogStatement statement = statements.get(0);
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
        loggers.forEach(Assertions::assertNotNull);
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
        final List<EventCategory> events = loggerService.events();

        boolean found = false;

        for (EventCategory eventCategoryTO : events) {
            if (UserLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("search"));
                assertFalse(eventCategoryTO.getEvents().contains("doCreate"));
                assertFalse(eventCategoryTO.getEvents().contains("setStatusOnWfAdapter"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (GroupLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("search"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
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
        for (EventCategory eventCategoryTO : events) {
            if (AnyTypeKind.USER.name().toLowerCase().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_LDAP.equals(eventCategoryTO.getSubcategory())
                        && EventCategoryType.PULL == eventCategoryTO.getType()) {

                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
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
        for (EventCategory eventCategoryTO : events) {
            if (EventCategoryType.TASK == eventCategoryTO.getType()
                    && "PullJobDelegate".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private static boolean logFileContains(final Path path, final String message, final int maxWaitSeconds)
            throws IOException {

        int i = 0;
        boolean messagePresent = false;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            String auditLog = Files.readString(path, StandardCharsets.UTF_8);
            messagePresent = auditLog.contains(message);

            i++;
        } while (!messagePresent && i < maxWaitSeconds);
        return messagePresent;
    }

    @Test
    public void customAuditAppender() throws IOException, InterruptedException {
        AuditLoggerName auditLoggerResUpd = new AuditLoggerName(
                EventCategoryType.LOGIC,
                ResourceLogic.class.getSimpleName(),
                null,
                "update",
                AuditElements.Result.SUCCESS);
        LoggerTO resUpd = new LoggerTO();
        resUpd.setKey(auditLoggerResUpd.toLoggerName());

        AuditLoggerName auditLoggerConnUpd = new AuditLoggerName(
                EventCategoryType.LOGIC,
                ConnectorLogic.class.getSimpleName(),
                null,
                "update",
                AuditElements.Result.SUCCESS);
        LoggerTO connUpd = new LoggerTO();
        connUpd.setKey(auditLoggerConnUpd.toLoggerName());

        try (InputStream propStream = getClass().getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            Path auditFilePath = Paths.get(props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_file.log");
            Files.write(auditFilePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);

            Path auditNoRewriteFilePath = Paths.get(props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_norewrite_file.log");
            Files.write(auditNoRewriteFilePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);

            // 1. Enable audit for resource update -> catched by FileRewriteAuditAppender
            resUpd.setLevel(LoggerLevel.DEBUG);
            loggerService.update(LoggerType.AUDIT, resUpd);

            // 2. Enable audit for connector update -> NOT catched by FileRewriteAuditAppender
            connUpd.setLevel(LoggerLevel.DEBUG);
            loggerService.update(LoggerType.AUDIT, connUpd);

            // 3. check that resource update is transformed and logged onto an audit file.
            ResourceTO resource = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(100);
            resourceService.update(resource);

            ConnInstanceTO connector = connectorService.readByResource(RESOURCE_NAME_CSV, null);
            assertNotNull(connector);
            connector.setPoolConf(new ConnPoolConfTO());
            connectorService.update(connector);

            // check audit_for_Master_file.log, it should contain only a static message
            assertTrue(logFileContains(auditFilePath,
                    "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                    + " - This is a static test message", 10));

            // nothing expected in audit_for_Master_norewrite_file.log instead
            assertFalse(logFileContains(auditNoRewriteFilePath,
                    "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                    + " - This is a static test message", 10));

            // clean audit_for_Master_file.log
            Files.write(auditFilePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            loggerService.delete(LoggerType.AUDIT, "syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]");

            resource = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(200);
            resourceService.update(resource);

            // check that nothing has been written to audit_for_Master_file.log
            assertTrue(StringUtils.isEmpty(Files.readString(auditFilePath, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            fail("Unable to read/write log files", e);
        } finally {
            resUpd.setLevel(LoggerLevel.ERROR);
            loggerService.update(LoggerType.AUDIT, resUpd);

            connUpd.setLevel(LoggerLevel.ERROR);
            loggerService.update(LoggerType.AUDIT, connUpd);
        }
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
        List<EventCategory> events = loggerService.events();
        assertNotNull(events);

        EventCategory userLogic = events.stream().
                filter(object -> "UserLogic".equals(object.getCategory())).findAny().get();
        assertNotNull(userLogic);
        assertEquals(1, userLogic.getEvents().stream().filter("create"::equals).count());
    }

    @Test
    public void issueSYNCOPE1446() {
        AuditLoggerName createSuccess = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name().toLowerCase(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                AuditElements.Result.SUCCESS);
        AuditLoggerName createFailure = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name().toLowerCase(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                AuditElements.Result.FAILURE);
        AuditLoggerName updateSuccess = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name().toLowerCase(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                AuditElements.Result.SUCCESS);
        AuditLoggerName updateFailure = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name().toLowerCase(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                AuditElements.Result.FAILURE);
        try {
            // 1. setup audit for propagation
            LoggerTO loggerTO = new LoggerTO();
            loggerTO.setKey(createSuccess.toLoggerName());
            loggerTO.setLevel(LoggerLevel.DEBUG);
            loggerService.update(LoggerType.AUDIT, loggerTO);

            loggerTO.setKey(createFailure.toLoggerName());
            loggerService.update(LoggerType.AUDIT, loggerTO);

            loggerTO.setKey(updateSuccess.toLoggerName());
            loggerService.update(LoggerType.AUDIT, loggerTO);

            loggerTO.setKey(updateFailure.toLoggerName());
            loggerService.update(LoggerType.AUDIT, loggerTO);

            // 2. push on resource
            PushTaskTO pushTask = new PushTaskTO();
            pushTask.setPerformCreate(true);
            pushTask.setPerformUpdate(true);
            pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);
            pushTask.setMatchingRule(MatchingRule.UPDATE);
            reconciliationService.push(new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).
                    anyKey("fc6dbc3a-6c07-4965-8781-921e7401a4a5").build(), pushTask);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail(e::getMessage);
        } finally {
            try {
                loggerService.delete(LoggerType.AUDIT, createSuccess.toLoggerName());
            } catch (Exception e) {
                // ignore
            }
            try {
                loggerService.delete(LoggerType.AUDIT, createFailure.toLoggerName());
            } catch (Exception e) {
                // ignore
            }
            try {
                loggerService.delete(LoggerType.AUDIT, updateSuccess.toLoggerName());
            } catch (Exception e) {
                // ignore
            }
            try {
                loggerService.delete(LoggerType.AUDIT, updateFailure.toLoggerName());
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
