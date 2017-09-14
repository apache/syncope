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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.rest.api.LoggerWrapper;
import org.apache.syncope.core.logic.ConnectorLogic;
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
        memoryAppenders.forEach(appender -> {
            assertNotNull(appender);
            assertNotNull(appender.getName());
        });
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
        loggers.forEach(logger -> {
            assertNotNull(logger);
        });
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
                assertTrue(eventCategoryTO.getEvents().contains("search"));
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
                assertTrue(eventCategoryTO.getEvents().contains("search"));
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
    public void customAuditAppender() throws IOException, InterruptedException {
        InputStream propStream = null;
        try {
            Properties props = new Properties();
            propStream = getClass().getResourceAsStream("/core-test.properties");
            props.load(propStream);

            String auditFilePath = props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_file.log";
            String auditNoRewriteFilePath = props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_norewrite_file.log";
            // 1. Enable audit for resource update -> catched by FileRewriteAuditAppender
            AuditLoggerName auditLoggerResUpd = new AuditLoggerName(
                    EventCategoryType.LOGIC,
                    ResourceLogic.class.getSimpleName(),
                    null,
                    "update",
                    AuditElements.Result.SUCCESS);

            LoggerTO loggerTOUpd = new LoggerTO();
            loggerTOUpd.setKey(auditLoggerResUpd.toLoggerName());
            loggerTOUpd.setLevel(LoggerLevel.DEBUG);
            loggerService.update(LoggerType.AUDIT, loggerTOUpd);

            // 2. Enable audit for connector update -> NOT catched by FileRewriteAuditAppender
            AuditLoggerName auditLoggerConnUpd = new AuditLoggerName(
                    EventCategoryType.LOGIC,
                    ConnectorLogic.class.getSimpleName(),
                    null,
                    "update",
                    AuditElements.Result.SUCCESS);

            LoggerTO loggerTOConnUpd = new LoggerTO();
            loggerTOConnUpd.setKey(auditLoggerConnUpd.toLoggerName());
            loggerTOConnUpd.setLevel(LoggerLevel.DEBUG);
            loggerService.update(LoggerType.AUDIT, loggerTOConnUpd);

            // 3. check that resource update is transformed and logged onto an audit file.
            ResourceTO resource = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(100);
            resourceService.update(resource);

            ConnInstanceTO connector = connectorService.readByResource(RESOURCE_NAME_CSV, null);
            assertNotNull(connector);
            connector.setPoolConf(new ConnPoolConfTO());
            connectorService.update(connector);

            File auditTempFile = new File(auditFilePath);
            // check audit_for_Master_file.log, it should contain only a static message
            String auditLog = FileUtils.readFileToString(auditTempFile, Charset.defaultCharset());

            assertTrue(StringUtils.contains(auditLog,
                    "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                    + " - This is a static test message"));
            File auditNoRewriteTempFile = new File(auditNoRewriteFilePath);
            // check audit_for_Master_file.log, it should contain only a static message
            String auditLogNoRewrite = FileUtils.readFileToString(auditNoRewriteTempFile, Charset.defaultCharset());

            assertFalse(StringUtils.contains(auditLogNoRewrite,
                    "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                    + " - This is a static test message"));

            // clean audit_for_Master_file.log
            FileUtils.writeStringToFile(auditTempFile, StringUtils.EMPTY, Charset.defaultCharset());
            loggerService.delete(LoggerType.AUDIT, "syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]");

            resource = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(200);
            resourceService.update(resource);

            // check that nothing has been written to audit_for_Master_file.log
            assertTrue(StringUtils.isEmpty(FileUtils.readFileToString(auditTempFile, Charset.defaultCharset())));
        } catch (IOException e) {
            fail("Unable to read/write log files" + e.getMessage());
        } finally {
            IOUtils.closeQuietly(propStream);
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
        List<EventCategoryTO> events = loggerService.events();
        assertNotNull(events);

        EventCategoryTO userLogic = events.stream().
                filter(object -> "UserLogic".equals(object.getCategory())).findAny().get();
        assertNotNull(userLogic);
        assertEquals(1, userLogic.getEvents().stream().filter(event -> "create".equals(event)).count());
    }
}
