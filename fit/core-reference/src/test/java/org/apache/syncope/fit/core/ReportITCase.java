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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.SampleReportJobDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public class ReportITCase extends AbstractITCase {

    protected static String execReport(final String reportKey) {
        Mutable<ReportTO> reportTO = new MutableObject<>(REPORT_SERVICE.read(reportKey));
        int preExecSize = reportTO.getValue().getExecutions().size();
        ExecTO execution = REPORT_SERVICE.execute(new ExecSpecs.Builder().key(reportKey).build());
        assertNotNull(execution.getExecutor());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                reportTO.setValue(REPORT_SERVICE.read(reportKey));
                return preExecSize < reportTO.getValue().getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        ExecTO exec = reportTO.getValue().getExecutions().stream().
                max(Comparator.comparing(ExecTO::getStart)).orElseThrow();
        assertEquals(ReportJob.Status.SUCCESS.name(), exec.getStatus());
        return exec.getKey();
    }

    @Test
    public void getReportDelegates() {
        Set<String> reportDelegates = ANONYMOUS_CLIENT.platform().
                getJavaImplInfo(IdRepoImplementationType.REPORT_DELEGATE).orElseThrow().getClasses();
        assertNotNull(reportDelegates);
        assertFalse(reportDelegates.isEmpty());
        assertTrue(reportDelegates.contains(SampleReportJobDelegate.class.getName()));
    }

    @Test
    public void list() {
        List<ReportTO> reports = REPORT_SERVICE.list();
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        reports.forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        ReportTO reportTO = REPORT_SERVICE.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");

        assertNotNull(reportTO);
        assertNotNull(reportTO.getExecutions());
        assertFalse(reportTO.getExecutions().isEmpty());
    }

    @Test
    public void crud() {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate" + getUUIDString());
        report.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        report.setFileExt("pdf");
        report.setJobDelegate(SampleReportJobDelegate.class.getSimpleName());
        report.setActive(true);

        report = createReport(report);
        assertNotNull(report);
        ReportTO actual = REPORT_SERVICE.read(report.getKey());
        assertNotNull(actual);
        assertEquals(actual, report);

        report = actual;
        report.setMimeType("text/csv");
        report.setFileExt("csv");

        REPORT_SERVICE.update(report);
        actual = REPORT_SERVICE.read(report.getKey());
        assertNotNull(actual);
        assertEquals("csv", actual.getFileExt());

        REPORT_SERVICE.delete(report.getKey());
        try {
            REPORT_SERVICE.read(report.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void executeAndExport() throws IOException {
        ReportTO report = REPORT_SERVICE.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        report.setActive(false);
        report.getExecutions().clear();
        REPORT_SERVICE.update(report);

        try {
            execReport(report.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Scheduling, e.getType());
            assertTrue(e.getElements().iterator().next().contains("active"));
        }

        report.setActive(true);
        REPORT_SERVICE.update(report);

        String execKey = execReport(report.getKey());

        Response response = REPORT_SERVICE.exportExecutionResult(execKey);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertNotNull(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).endsWith(".pdf"));

        assertFalse(response.readEntity(String.class).isEmpty());
    }

    @Test
    public void deleteExecutions() throws IOException {
        OffsetDateTime start = OffsetDateTime.now();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }

        ReportTO report = REPORT_SERVICE.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        report.setName("deleteExecutions" + getUUIDString());
        report.getExecutions().clear();
        report = createReport(report);
        assertNotNull(report);

        String execKey = execReport(report.getKey());
        assertNotNull(execKey);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        OffsetDateTime end = OffsetDateTime.now();

        Response response = REPORT_SERVICE.deleteExecutions(
                new ExecQuery.Builder().key(report.getKey()).after(start).before(end).build());
        List<BatchResponseItem> batchResponseItems = parseBatchResponse(response);
        assertEquals(1, batchResponseItems.size());
        assertEquals(execKey, batchResponseItems.getFirst().getHeaders().get(RESTHeaders.RESOURCE_KEY).getFirst());
        assertEquals(Response.Status.OK.getStatusCode(), batchResponseItems.getFirst().getStatus());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = REPORT_SERVICE.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        reportTO.setName("issueSYNCOPE102" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO.getKey());
        String reportKey = reportTO.getKey();

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            assertNotNull(REPORT_SERVICE.execute(new ExecSpecs.Builder().key(reportKey).build()));
        }

        // Wait for one execution
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return !REPORT_SERVICE.read(reportKey).getExecutions().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
