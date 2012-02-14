/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.syncope.client.report.UserReportletConf;
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.client.to.UserTO;

public class ReportTestITCase extends AbstractTest {

    @Test
    public void count() {
        Integer count = restTemplate.getForObject(
                BASE_URL + "report/count.json", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<ReportTO> reports = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "report/list", ReportTO[].class));
        assertNotNull(reports);
        assertFalse(reports.isEmpty());
        for (ReportTO report : reports) {
            assertNotNull(report);
        }
    }

    @Test
    public void listExecutions() {
        List<ReportExecTO> executions = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "report/execution/list",
                ReportExecTO[].class));
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (ReportExecTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public void read() {
        ReportTO reportTO = restTemplate.getForObject(
                BASE_URL + "report/read/{reportId}", ReportTO.class, 1);

        assertNotNull(reportTO);
        assertNotNull(reportTO.getExecutions());
        assertFalse(reportTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        ReportExecTO reportExecTO = restTemplate.getForObject(
                BASE_URL + "report/execution/read/{reportId}",
                ReportExecTO.class, 1);
        assertNotNull(reportExecTO);
    }

    @Test
    public void create() {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = restTemplate.postForObject(
                BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);

        ReportTO actual = restTemplate.getForObject(
                BASE_URL + "report/read/{reportId}", ReportTO.class,
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

        report = restTemplate.postForObject(
                BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);
        assertEquals(2, report.getReportletConfs().size());

        report.addReportletConf(new UserReportletConf("last"));

        ReportTO updated = restTemplate.postForObject(
                BASE_URL + "report/update", report, ReportTO.class);
        assertNotNull(updated);
        assertEquals(3, updated.getReportletConfs().size());
    }

    @Test
    public void delete() {
        ReportTO report = new ReportTO();
        report.setName("testReportForDelete");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = restTemplate.postForObject(
                BASE_URL + "report/create", report, ReportTO.class);
        assertNotNull(report);

        restTemplate.delete(
                BASE_URL + "report/delete/{reportId}", report.getId());

        try {
            restTemplate.getForObject(BASE_URL + "report/read/{reportId}",
                    UserTO.class, report.getId());
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void executeAndExport()
            throws IOException {

        ReportTO reportTO = restTemplate.getForObject(
                BASE_URL + "report/read/{reportId}", ReportTO.class, 1);
        assertNotNull(reportTO);

        Set<Long> preExecIds = new HashSet<Long>();
        for (ReportExecTO exec : reportTO.getExecutions()) {
            preExecIds.add(exec.getId());
        }

        ReportExecTO execution = restTemplate.postForObject(
                BASE_URL + "report/execute/{reportId}",
                null, ReportExecTO.class, reportTO.getId());
        assertNotNull(execution);

        int i = 0;
        int maxit = 20;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = restTemplate.getForObject(
                    BASE_URL + "report/read/{reportId}", ReportTO.class, 1);

            i++;
        } while (preExecIds.size() == reportTO.getExecutions().size()
                && i < maxit);

        Set<Long> postExecIds = new HashSet<Long>();
        for (ReportExecTO exec : reportTO.getExecutions()) {
            postExecIds.add(exec.getId());
        }

        postExecIds.removeAll(preExecIds);
        assertEquals(1, postExecIds.size());

        // wait for report exec XML to be stored...
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        HttpGet getMethod = new HttpGet(BASE_URL + "report/execution/export/"
                + postExecIds.iterator().next());
        HttpResponse response =
                ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory()).getHttpClient().execute(getMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        String export = EntityUtils.toString(response.getEntity()).trim();
        assertNotNull(export);
        assertFalse(export.isEmpty());
    }
}
