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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.report.AuditReportletConf;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecDeleteQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReportITCase extends AbstractITCase {

    protected static String execReport(final String reportKey) {
        AtomicReference<ReportTO> reportTO = new AtomicReference<>(reportService.read(reportKey));
        int preExecSize = reportTO.get().getExecutions().size();
        ExecTO execution = reportService.execute(new ExecSpecs.Builder().key(reportKey).build());
        assertNotNull(execution.getExecutor());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                reportTO.set(reportService.read(reportKey));
                return preExecSize < reportTO.get().getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });
        ExecTO exec = reportTO.get().getExecutions().get(reportTO.get().getExecutions().size() - 1);
        assertEquals(ReportExecStatus.SUCCESS.name(), exec.getStatus());
        return exec.getKey();
    }

    @Test
    public void getReportletConfs() {
        Set<String> reportletConfs = adminClient.platform().
                getJavaImplInfo(IdRepoImplementationType.REPORTLET).get().getClasses();
        assertNotNull(reportletConfs);
        assertFalse(reportletConfs.isEmpty());
        assertTrue(reportletConfs.contains(UserReportletConf.class.getName()));
    }

    @Test
    public void list() {
        List<ReportTO> reports = reportService.list();
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        reports.forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        ReportTO reportTO = reportService.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");

        assertNotNull(reportTO);
        assertNotNull(reportTO.getExecutions());
        assertFalse(reportTO.getExecutions().isEmpty());
    }

    @Test
    public void create() {
        ImplementationTO reportlet1 = new ImplementationTO();
        reportlet1.setKey("UserReportletConf" + getUUIDString());
        reportlet1.setEngine(ImplementationEngine.JAVA);
        reportlet1.setType(IdRepoImplementationType.REPORTLET);
        reportlet1.setBody(POJOHelper.serialize(new UserReportletConf("first")));
        Response response = implementationService.create(reportlet1);
        reportlet1.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ImplementationTO reportlet2 = new ImplementationTO();
        reportlet2.setKey("UserReportletConf" + getUUIDString());
        reportlet2.setEngine(ImplementationEngine.JAVA);
        reportlet2.setType(IdRepoImplementationType.REPORTLET);
        reportlet2.setBody(POJOHelper.serialize(new UserReportletConf("second")));
        response = implementationService.create(reportlet2);
        reportlet2.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ReportTO report = new ReportTO();
        report.setName("testReportForCreate" + getUUIDString());
        report.getReportlets().add(reportlet1.getKey());
        report.getReportlets().add(reportlet2.getKey());
        report.setTemplate("sample");

        report = createReport(report);
        assertNotNull(report);
        ReportTO actual = reportService.read(report.getKey());
        assertNotNull(actual);
        assertEquals(actual, report);
    }

    @Test
    public void update() {
        ImplementationTO reportlet1 = new ImplementationTO();
        reportlet1.setKey("UserReportletConf" + getUUIDString());
        reportlet1.setEngine(ImplementationEngine.JAVA);
        reportlet1.setType(IdRepoImplementationType.REPORTLET);
        reportlet1.setBody(POJOHelper.serialize(new UserReportletConf("first")));
        Response response = implementationService.create(reportlet1);
        reportlet1.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ImplementationTO reportlet2 = new ImplementationTO();
        reportlet2.setKey("UserReportletConf" + getUUIDString());
        reportlet2.setEngine(ImplementationEngine.JAVA);
        reportlet2.setType(IdRepoImplementationType.REPORTLET);
        reportlet2.setBody(POJOHelper.serialize(new UserReportletConf("second")));
        response = implementationService.create(reportlet2);
        reportlet2.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ReportTO report = new ReportTO();
        report.setName("testReportForUpdate" + getUUIDString());
        report.getReportlets().add(reportlet1.getKey());
        report.getReportlets().add(reportlet2.getKey());
        report.setTemplate("sample");

        report = createReport(report);
        assertNotNull(report);
        assertEquals(2, report.getReportlets().size());

        ImplementationTO reportlet3 = new ImplementationTO();
        reportlet3.setKey("UserReportletConf" + getUUIDString());
        reportlet3.setEngine(ImplementationEngine.JAVA);
        reportlet3.setType(IdRepoImplementationType.REPORTLET);
        reportlet3.setBody(POJOHelper.serialize(new UserReportletConf("last")));
        response = implementationService.create(reportlet3);
        reportlet3.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        report.getReportlets().add(reportlet3.getKey());

        reportService.update(report);
        ReportTO updated = reportService.read(report.getKey());
        assertNotNull(updated);
        assertEquals(3, updated.getReportlets().size());
    }

    @Test
    public void delete() {
        ImplementationTO reportlet1 = new ImplementationTO();
        reportlet1.setKey("UserReportletConf" + getUUIDString());
        reportlet1.setEngine(ImplementationEngine.JAVA);
        reportlet1.setType(IdRepoImplementationType.REPORTLET);
        reportlet1.setBody(POJOHelper.serialize(new UserReportletConf("first")));
        Response response = implementationService.create(reportlet1);
        reportlet1.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ImplementationTO reportlet2 = new ImplementationTO();
        reportlet2.setKey("UserReportletConf" + getUUIDString());
        reportlet2.setEngine(ImplementationEngine.JAVA);
        reportlet2.setType(IdRepoImplementationType.REPORTLET);
        reportlet2.setBody(POJOHelper.serialize(new UserReportletConf("second")));
        response = implementationService.create(reportlet2);
        reportlet2.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        ReportTO report = new ReportTO();
        report.setName("testReportForDelete" + getUUIDString());
        report.getReportlets().add(reportlet1.getKey());
        report.getReportlets().add(reportlet2.getKey());
        report.setTemplate("sample");

        report = createReport(report);
        assertNotNull(report);

        reportService.delete(report.getKey());

        try {
            reportService.read(report.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    private static void checkExport(final String execKey, final ReportExecExportFormat fmt) throws IOException {
        Response response = reportService.exportExecutionResult(execKey, fmt);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertNotNull(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).endsWith('.' + fmt.name().toLowerCase()));

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        assertFalse(IOUtils.toString((InputStream) entity, StandardCharsets.UTF_8.name()).isEmpty());
    }

    @Test
    public void executeAndExport() throws IOException {
        ReportTO reportTO = reportService.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        reportTO.setName("executeAndExport" + getUUIDString());
        reportTO.setActive(false);
        reportTO.getExecutions().clear();
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        try {
            execReport(reportTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Scheduling, e.getType());
            assertTrue(e.getElements().iterator().next().contains("active"));
        }

        reportTO.setActive(true);
        reportService.update(reportTO);

        String execKey = execReport(reportTO.getKey());

        checkExport(execKey, ReportExecExportFormat.XML);
        checkExport(execKey, ReportExecExportFormat.HTML);
        checkExport(execKey, ReportExecExportFormat.PDF);
        checkExport(execKey, ReportExecExportFormat.RTF);
        checkExport(execKey, ReportExecExportFormat.CSV);
    }

    @Test
    public void deleteExecutions() throws IOException {
        OffsetDateTime start = OffsetDateTime.now();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }

        ReportTO reportTO = reportService.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        reportTO.setName("deleteExecutions" + getUUIDString());
        reportTO.getExecutions().clear();
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        String execKey = execReport(reportTO.getKey());
        assertNotNull(execKey);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        OffsetDateTime end = OffsetDateTime.now();

        Response response = reportService.deleteExecutions(
                new ExecDeleteQuery.Builder().key(reportTO.getKey()).startedAfter(start).endedBefore(end).build());
        List<BatchResponseItem> batchResponseItems = parseBatchResponse(response);
        assertEquals(1, batchResponseItems.size());
        assertEquals(execKey, batchResponseItems.get(0).getHeaders().get(RESTHeaders.RESOURCE_KEY).get(0));
        assertEquals(Response.Status.OK.getStatusCode(), batchResponseItems.get(0).getStatus());
    }

    @Test
    public void auditReport() throws IOException {
        AuditLoggerName auditLoggerName = new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                "UserLogic",
                null,
                "selfRead",
                AuditElements.Result.SUCCESS);

        try {
            AuditConfTO audit = new AuditConfTO();
            audit.setKey(auditLoggerName.toAuditKey());
            auditService.create(audit);

            ImplementationTO auditReportlet = new ImplementationTO();
            auditReportlet.setKey("UserReportletConf" + getUUIDString());
            auditReportlet.setEngine(ImplementationEngine.JAVA);
            auditReportlet.setType(IdRepoImplementationType.REPORTLET);
            auditReportlet.setBody(POJOHelper.serialize(new AuditReportletConf("auditReportlet" + getUUIDString())));
            Response response = implementationService.create(auditReportlet);
            auditReportlet.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

            ReportTO report = new ReportTO();
            report.setName("auditReport" + getUUIDString());
            report.setActive(true);
            report.getReportlets().add(auditReportlet.getKey());
            report.setTemplate("sample");
            report = createReport(report);

            String execKey = execReport(report.getKey());
            checkExport(execKey, ReportExecExportFormat.XML);

            report = reportService.read(report.getKey());
            assertNotNull(report.getLastExec());
        } finally {
            auditService.delete(auditLoggerName.toAuditKey());
        }
    }

    @Test
    public void issueSYNCOPE43() {
        ReportTO reportTO = new ReportTO();
        reportTO.setName("issueSYNCOPE43" + getUUIDString());
        reportTO.setActive(true);
        reportTO.setTemplate("sample");
        reportTO = createReport(reportTO);
        assertNotNull(reportTO.getKey());

        execReport(reportTO.getKey());
        reportTO = reportService.read(reportTO.getKey());
        assertEquals(1, reportTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = reportService.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        reportTO.setName("issueSYNCOPE102" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO.getKey());
        String reportKey = reportTO.getKey();

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            assertNotNull(reportService.execute(new ExecSpecs.Builder().key(reportKey).build()));
        }

        // Wait for one execution
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return !reportService.read(reportKey).getExecutions().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
