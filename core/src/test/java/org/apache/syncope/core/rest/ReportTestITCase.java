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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.report.UserReportletConf;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ReportletConfClasses;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.apache.syncope.common.types.ReportExecStatus;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ReportTestITCase extends AbstractTest {

    public ReportTO createReport(final ReportTO report) {
        Response response = reportService.create(report);
        assertCreated(response);
        return adminClient.getObject(response.getLocation(), ReportService.class, ReportTO.class);
    }

    @Test
    public void getReportletClasses() {
        ReportletConfClasses reportletClasses = reportService.getReportletConfClasses();
        assertNotNull(reportletClasses);
        assertFalse(reportletClasses.getConfClasses().isEmpty());
    }

    @Test
    public void count() {
        Integer count = reportService.count();
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<ReportTO> reports = reportService.list();
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        for (ReportTO report : reports) {
            assertNotNull(report);
        }
    }

    @Test
    public void read() {
        ReportTO reportTO = reportService.read(1L);

        assertNotNull(reportTO);
        assertNotNull(reportTO.getExecutions());
        assertFalse(reportTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        ReportExecTO reportExecTO = reportService.readExecution(1L);
        assertNotNull(reportExecTO);
    }

    @Test
    public void create() {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate" + getUUIDString());
        report.getReportletConfs().add(new UserReportletConf("first"));
        report.getReportletConfs().add(new UserReportletConf("second"));

        report = createReport(report);
        assertNotNull(report);

        ReportTO actual = reportService.read(report.getId());
        assertNotNull(actual);

        assertEquals(actual, report);
    }

    @Test
    public void update() {
        ReportTO report = new ReportTO();
        report.setName("testReportForUpdate" + getUUIDString());
        report.getReportletConfs().add(new UserReportletConf("first"));
        report.getReportletConfs().add(new UserReportletConf("second"));

        report = createReport(report);
        assertNotNull(report);
        assertEquals(2, report.getReportletConfs().size());

        report.getReportletConfs().add(new UserReportletConf("last"));

        reportService.update(report.getId(), report);
        ReportTO updated = reportService.read(report.getId());
        assertNotNull(updated);
        assertEquals(3, updated.getReportletConfs().size());
    }

    @Test
    public void delete() {
        ReportTO report = new ReportTO();
        report.setName("testReportForDelete" + getUUIDString());
        report.getReportletConfs().add(new UserReportletConf("first"));
        report.getReportletConfs().add(new UserReportletConf("second"));

        report = createReport(report);
        assertNotNull(report);

        reportService.delete(report.getId());

        try {
            reportService.read(report.getId());
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
        }
    }

    private void checkExport(final Long execId, final ReportExecExportFormat fmt) throws IOException {
        final Response response = reportService.exportExecutionResult(execId, fmt);
        assertNotNull(response);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNotNull(response.getHeaderString(SyncopeConstants.CONTENT_DISPOSITION_HEADER));
        assertTrue(response.getHeaderString(SyncopeConstants.CONTENT_DISPOSITION_HEADER).
                endsWith("." + fmt.name().toLowerCase()));

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        assertFalse(IOUtils.toString((InputStream) entity, SyncopeConstants.DEFAULT_ENCODING).isEmpty());
    }

    @Test
    public void executeAndExport() throws IOException {
        ReportTO reportTO = reportService.read(1L);
        reportTO.setId(0);
        reportTO.setName("executeAndExport" + getUUIDString());
        reportTO.getExecutions().clear();
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        ReportExecTO execution = reportService.execute(reportTO.getId());
        assertNotNull(execution);

        int i = 0;
        int maxit = 50;

        // wait for report execution completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getId());

            assertNotNull(reportTO);
            assertNotNull(reportTO.getExecutions());

            i++;
        } while (!ReportExecStatus.SUCCESS.name().equals(reportTO.getExecutions().get(0).getStatus()) && i < maxit);
        assertEquals(ReportExecStatus.SUCCESS.name(), reportTO.getExecutions().get(0).getStatus());

        long execId = reportTO.getExecutions().get(0).getId();

        checkExport(execId, ReportExecExportFormat.XML);
        checkExport(execId, ReportExecExportFormat.HTML);
        checkExport(execId, ReportExecExportFormat.PDF);
        checkExport(execId, ReportExecExportFormat.RTF);
    }

    @Test
    public void issueSYNCOPE43() {
        ReportTO reportTO = new ReportTO();
        reportTO.setName("issueSYNCOPE43" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        ReportExecTO execution = reportService.execute(reportTO.getId());
        assertNotNull(execution);

        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getId());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);

        assertEquals(1, reportTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = reportService.read(1L);
        reportTO.setId(0);
        reportTO.setName("issueSYNCOPE102" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            ReportExecTO execution = reportService.execute(reportTO.getId());
            assertNotNull(execution);
        }

        // Wait for one execution
        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getId());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);
        assertFalse(reportTO.getExecutions().isEmpty());
    }
}
