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

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.apache.syncope.report.UserReportletConf;
import org.apache.syncope.to.ReportExecTO;
import org.apache.syncope.to.ReportTO;
import org.apache.syncope.to.UserTO;

public class ReportTestITCase extends AbstractTest {

    @Override
    public void setupService() {
    }

    @Test
    public void getReportletClasses() {
        Set<String> reportletClasses = restTemplate.getForObject(BASE_URL + "report/reportletConfClasses.json",
                Set.class);
        assertNotNull(reportletClasses);
        assertFalse(reportletClasses.isEmpty());
    }

    @Test
    public void count() {
        Integer count = restTemplate.getForObject(BASE_URL + "report/count.json", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<ReportTO> reports = Arrays.asList(restTemplate.getForObject(BASE_URL + "report/list", ReportTO[].class));
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        for (ReportTO report : reports) {
            assertNotNull(report);
        }
    }

    @Test
    public void listExecutions() {
        List<ReportExecTO> executions = Arrays.asList(restTemplate.getForObject(BASE_URL + "report/execution/list",
                ReportExecTO[].class));
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (ReportExecTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public void read() {
        ReportTO reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);

        assertNotNull(reportTO);
        assertNotNull(reportTO.getExecutions());
        assertFalse(reportTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        ReportExecTO reportExecTO = restTemplate.getForObject(BASE_URL + "report/execution/read/{reportId}",
                ReportExecTO.class, 1);
        assertNotNull(reportExecTO);
    }

    @Test
    public void create() {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = restTemplate.postForObject(BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);

        ReportTO actual = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class,
                report.getId());
        assertNotNull(actual);

        assertEquals(actual, report);
    }

    @Test
    public void update() {
        ReportTO report = new ReportTO();
        report.setName("testReportForUpdate");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = restTemplate.postForObject(BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);
        assertEquals(2, report.getReportletConfs().size());

        report.addReportletConf(new UserReportletConf("last"));

        ReportTO updated = restTemplate.postForObject(BASE_URL + "report/update", report, ReportTO.class);
        assertNotNull(updated);
        assertEquals(3, updated.getReportletConfs().size());
    }

    @Test
    public void delete() {
        ReportTO report = new ReportTO();
        report.setName("testReportForDelete");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = restTemplate.postForObject(BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);

        ReportTO deletedReport =
                restTemplate.getForObject(BASE_URL + "report/delete/{reportId}", ReportTO.class, report.getId());
        assertNotNull(deletedReport);

        try {
            restTemplate.getForObject(BASE_URL + "report/read/{reportId}", UserTO.class, report.getId());
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    private void checkExport(final long execId, final String fmt, final String encodedAuth) throws IOException {
        URL url = new URL(BASE_URL + "report/execution/export/" + execId + "?fmt=" + fmt);
        int responseCode = 0;
        String export = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            StringWriter writer = new StringWriter();
            IOUtils.copy(connection.getInputStream(), writer);
            export = writer.toString();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            LOG.error("This should be a temporary exception: ignore", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        assertEquals(200, responseCode);
        assertNotNull(export);
        assertFalse(export.isEmpty());
    }

    @Test
    public void executeAndExport()
            throws IOException {

        ReportTO reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);
        reportTO.setId(0);
        reportTO.setName("executeAndExport");
        reportTO = restTemplate.postForObject(BASE_URL + "report/create", reportTO, ReportTO.class);
        assertNotNull(reportTO);

        ReportExecTO execution = restTemplate.postForObject(BASE_URL + "report/execute/{reportId}", null,
                ReportExecTO.class, reportTO.getId());
        assertNotNull(execution);

        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, reportTO.getId());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);

        long execId = reportTO.getExecutions().iterator().next().getId();

        // Export
        String encodedAuth = Base64.encodeBase64String((ADMIN_UID + ":" + ADMIN_PWD).getBytes());
        URL url = new URL(BASE_URL + "report/execution/export/" + execId);

        // 1. XML
        maxit = 30;
        int responseCode = 0;
        String export = null;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            maxit--;

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

                StringWriter writer = new StringWriter();
                IOUtils.copy(connection.getInputStream(), writer);
                export = writer.toString();
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                LOG.error("This should be a temporary exception: ignore", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } while (responseCode != 200 && maxit > 0);
        assertEquals(200, responseCode);
        assertNotNull(export);
        assertFalse(export.isEmpty());

        // 2. HTML
        checkExport(execId, "HTML", encodedAuth);

        // 3. PDF
        checkExport(execId, "PDF", encodedAuth);

        // 4. RTF
        checkExport(execId, "RTF", encodedAuth);
    }

    @Test
    public void issueSYNCOPE43() {
        ReportTO reportTO = new ReportTO();
        reportTO.setName("issueSYNCOPE43");
        reportTO = restTemplate.postForObject(BASE_URL + "report/create", reportTO, ReportTO.class);
        assertNotNull(reportTO);

        ReportExecTO execution = restTemplate.postForObject(BASE_URL + "report/execute/{reportId}", null,
                ReportExecTO.class, reportTO.getId());
        assertNotNull(execution);

        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, reportTO.getId());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);

        assertEquals(1, reportTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);
        reportTO.setId(0);
        reportTO.setName("issueSYNCOPE102");
        reportTO = restTemplate.postForObject(BASE_URL + "report/create", reportTO, ReportTO.class);
        assertNotNull(reportTO);

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            ReportExecTO execution = restTemplate.postForObject(BASE_URL + "report/execute/{reportId}", null,
                    ReportExecTO.class, reportTO.getId());
            assertNotNull(execution);
        }

        // Wait for one execution
        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, reportTO.getId());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);
        assertFalse(reportTO.getExecutions().isEmpty());
    }
}
