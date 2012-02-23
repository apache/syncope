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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

@Component
public class ReportRestClient extends AbstractBaseRestClient
        implements ExecutionRestClient {

    public Set<String> getReportletClasses() {
        Set<String> reportletClasses = null;

        try {
            reportletClasses = restTemplate.getForObject(
                    baseURL + "report/reportletClasses.json", Set.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting available reportlet classes", e);
        }
        return reportletClasses;
    }

    public List<ReportTO> list() {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "report/list", ReportTO[].class));
    }

    public List<ReportTO> list(final int page, final int size) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "report/list/{page}/{size}.json",
                ReportTO[].class, page, size));
    }

    public int count() {
        return restTemplate.getForObject(
                baseURL + "report/count.json", Integer.class);
    }

    public ReportTO create(final ReportTO reportTO) {
        return restTemplate.postForObject(
                baseURL + "report/create", reportTO, ReportTO.class);
    }

    public ReportTO update(final ReportTO reportTO) {
        return restTemplate.postForObject(
                baseURL + "report/update", reportTO, ReportTO.class);
    }

    /**
     * Delete specified report.
     *
     * @param reportId report to delete
     */
    public void delete(final Long reportId) {
        restTemplate.delete(
                baseURL + "report/delete/{reportId}", reportId);
    }

    /**
     * Start execution for the specified report.
     *
     * @param reportId report id
     */
    @Override
    public void startExecution(final Long reportId) {
        restTemplate.postForObject(
                baseURL + "report/execute/{reportId}",
                null, ReportExecTO.class, reportId);
    }

    /**
     * Delete specified report execution.
     *
     * @param reportExecId report execution id
     */
    @Override
    public void deleteExecution(final Long reportExecId) {
        restTemplate.delete(baseURL
                + "report/execution/delete/{execId}", reportExecId);
    }

    /**
     * Get all executions.
     *
     * @return list of all executions
     */
    @Override
    public List<ReportExecTO> listExecutions() {
        return Arrays.asList(
                restTemplate.getForObject(
                baseURL + "report/execution/list",
                ReportExecTO[].class));
    }
}
