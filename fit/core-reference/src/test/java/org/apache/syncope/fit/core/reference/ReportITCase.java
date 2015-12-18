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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.common.rest.api.beans.BulkExecDeleteQuery;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ReportITCase extends AbstractITCase {

    private ReportTO createReport(final ReportTO report) {
        Response response = reportService.create(report);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        return getObject(response.getLocation(), ReportService.class, ReportTO.class);
    }

    @Test
    public void getReportletClasses() {
        Set<String> reportlets = syncopeService.info().getReportlets();
        assertNotNull(reportlets);
        assertFalse(reportlets.isEmpty());
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
    public void create() {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate" + getUUIDString());
        report.getReportletConfs().add(new UserReportletConf("first"));
        report.getReportletConfs().add(new UserReportletConf("second"));

        report = createReport(report);
        assertNotNull(report);

        ReportTO actual = reportService.read(report.getKey());
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

        reportService.update(report);
        ReportTO updated = reportService.read(report.getKey());
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

        reportService.delete(report.getKey());

        try {
            reportService.read(report.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    private Long execute(final Long reportKey) {
        ReportExecTO execution = reportService.execute(new ExecuteQuery.Builder().key(reportKey).build());
        assertNotNull(execution);

        int i = 0;
        int maxit = 50;

        ReportTO reportTO;

        // wait for report execution completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportKey);

            assertNotNull(reportTO);
            assertNotNull(reportTO.getExecutions());

            i++;
        } while (reportTO.getExecutions().isEmpty()
                || (!ReportExecStatus.SUCCESS.name().equals(reportTO.getExecutions().get(0).getStatus()) && i < maxit));
        assertEquals(ReportExecStatus.SUCCESS.name(), reportTO.getExecutions().get(0).getStatus());

        return reportTO.getExecutions().get(0).getKey();
    }

    private void checkExport(final Long execId, final ReportExecExportFormat fmt) throws IOException {
        Response response = reportService.exportExecutionResult(execId, fmt);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertNotNull(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).
                endsWith("." + fmt.name().toLowerCase()));

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        assertFalse(IOUtils.toString((InputStream) entity, SyncopeConstants.DEFAULT_ENCODING).isEmpty());
    }

    @Test
    public void executeAndExport() throws IOException {
        ReportTO reportTO = reportService.read(1L);
        reportTO.setKey(0);
        reportTO.setName("executeAndExport" + getUUIDString());
        reportTO.setActive(false);
        reportTO.getExecutions().clear();
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        try {
            execute(reportTO.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Scheduling, e.getType());
            assertTrue(e.getElements().iterator().next().contains("active"));
        }

        reportTO.setActive(true);
        reportService.update(reportTO);

        long execId = execute(reportTO.getKey());

        checkExport(execId, ReportExecExportFormat.XML);
        checkExport(execId, ReportExecExportFormat.HTML);
        checkExport(execId, ReportExecExportFormat.PDF);
        checkExport(execId, ReportExecExportFormat.RTF);
        checkExport(execId, ReportExecExportFormat.CSV);
    }

    @Test
    public void deleteExecutions() {
        Date start = new Date();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        ReportTO reportTO = reportService.read(1L);
        reportTO.setKey(0);
        reportTO.setName("deleteExecutions" + getUUIDString());
        reportTO.getExecutions().clear();
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        Long execId = execute(reportTO.getKey());
        assertNotNull(execId);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        Date end = new Date();

        BulkActionResult result = reportService.deleteExecutions(
                new BulkExecDeleteQuery.Builder().key(reportTO.getKey()).startedAfter(start).endedBefore(end).build());
        assertNotNull(result);

        assertEquals(1, result.getResults().size());
        assertEquals(execId.toString(), result.getResults().keySet().iterator().next());
        assertEquals(BulkActionResult.Status.SUCCESS, result.getResults().entrySet().iterator().next().getValue());
    }

    @Test
    public void issueSYNCOPE43() {
        ReportTO reportTO = new ReportTO();
        reportTO.setName("issueSYNCOPE43" + getUUIDString());
        reportTO.setActive(true);
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        ReportExecTO execution = reportService.execute(new ExecuteQuery.Builder().key(reportTO.getKey()).build());
        assertNotNull(execution);

        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getKey());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);

        assertEquals(1, reportTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = reportService.read(1L);
        reportTO.setKey(0);
        reportTO.setName("issueSYNCOPE102" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            ReportExecTO execution = reportService.execute(new ExecuteQuery.Builder().key(reportTO.getKey()).build());
            assertNotNull(execution);
        }

        // Wait for one execution
        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getKey());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);
        assertFalse(reportTO.getExecutions().isEmpty());
    }
}
