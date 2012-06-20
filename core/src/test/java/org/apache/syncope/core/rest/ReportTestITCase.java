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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.client.report.UserReportletConf;
import org.apache.syncope.client.to.ReportExecTO;
import org.apache.syncope.client.to.ReportTO;
import org.apache.syncope.client.to.UserTO;

public class ReportTestITCase extends AbstractTest {

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

    @Test
    public void executeAndExport()
            throws IOException {

        ReportTO reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);
        assertNotNull(reportTO);

        Set<Long> preExecIds = new HashSet<Long>();
        for (ReportExecTO exec : reportTO.getExecutions()) {
            preExecIds.add(exec.getId());
        }

        ReportExecTO execution = restTemplate.postForObject(BASE_URL + "report/execute/{reportId}", null,
                ReportExecTO.class, reportTO.getId());
        assertNotNull(execution);

        int maxit = 50;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);

            maxit--;
        } while (preExecIds.size() == reportTO.getExecutions().size() && maxit > 0);

        Set<Long> postExecIds = new HashSet<Long>();
        for (ReportExecTO exec : reportTO.getExecutions()) {
            postExecIds.add(exec.getId());
        }

        postExecIds.removeAll(preExecIds);
        assertEquals(1, postExecIds.size());

        // Export
        // 1. XML (default)

        final HttpClient client = ((PreemptiveAuthHttpRequestFactory) restTemplate.getRequestFactory()).getHttpClient();
        final AuthScope scope = ((PreemptiveAuthHttpRequestFactory) restTemplate.getRequestFactory()).getAuthScope();
        final HttpHost targetHost = new HttpHost(scope.getHost(), scope.getPort(), scope.getScheme());


        // Add AuthCache to the execution context
        BasicHttpContext localcontext = new BasicHttpContext();

        // Generate BASIC scheme object and add it to the local auth cache
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        HttpResponse response = null;

        maxit = 10;

        // issueSYNCOPE89
        ((ThreadSafeClientConnManager) client.getConnectionManager()).setDefaultMaxPerRoute(10);

        HttpGet getMethod = new HttpGet(BASE_URL + "report/execution/export/" + postExecIds.iterator().next());

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            response = client.execute(targetHost, getMethod, localcontext);

            maxit--;
        } while ((response == null || response.getStatusLine().getStatusCode() != 200) && maxit > 0);

        assertEquals(200, response.getStatusLine().getStatusCode());

        String export = EntityUtils.toString(response.getEntity()).trim();
        assertNotNull(export);
        assertFalse(export.isEmpty());

        // 2. HTML
        getMethod = new HttpGet(BASE_URL + "report/execution/export/" + postExecIds.iterator().next() + "?fmt=HTML");
        response = client.execute(targetHost, getMethod, localcontext);
        assertEquals(200, response.getStatusLine().getStatusCode());

        export = EntityUtils.toString(response.getEntity()).trim();
        assertNotNull(export);
        assertFalse(export.isEmpty());

        // 3. PDF
        getMethod = new HttpGet(BASE_URL + "report/execution/export/" + postExecIds.iterator().next() + "?fmt=PDF");
        response = client.execute(targetHost, getMethod, localcontext);
        assertEquals(200, response.getStatusLine().getStatusCode());

        export = EntityUtils.toString(response.getEntity()).trim();
        assertNotNull(export);
        assertFalse(export.isEmpty());

        // 4. RTF
        getMethod = new HttpGet(BASE_URL + "report/execution/export/" + postExecIds.iterator().next() + "?fmt=RTF");
        response = client.execute(targetHost, getMethod, localcontext);
        assertEquals(200, response.getStatusLine().getStatusCode());

        export = EntityUtils.toString(response.getEntity()).trim();
        assertNotNull(export);
        assertFalse(export.isEmpty());
    }

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

            reportTO = restTemplate.getForObject(BASE_URL + "report/read/{reportId}", ReportTO.class, 1);

            maxit--;
        } while (reportTO.getExecutions().size() == 0 && maxit > 0);

        assertEquals(1, reportTO.getExecutions().size());
    }
}
