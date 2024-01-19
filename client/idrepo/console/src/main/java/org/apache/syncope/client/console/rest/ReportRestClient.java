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
package org.apache.syncope.client.console.rest;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class ReportRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 1644689667998953604L;

    public ReportTO read(final String reportKey) {
        return getService(ReportService.class).read(reportKey);
    }

    public List<ReportTO> list() {
        return getService(ReportService.class).list();
    }

    public JobTO getJob(final String key) {
        return getService(ReportService.class).getJob(key);
    }

    public List<JobTO> listJobs() {
        return getService(ReportService.class).listJobs();
    }

    public void actionJob(final String refKey, final JobAction jobAction) {
        getService(ReportService.class).actionJob(refKey, jobAction);
    }

    public void create(final ReportTO reportTO) {
        getService(ReportService.class).create(reportTO);
    }

    public void update(final ReportTO reportTO) {
        getService(ReportService.class).update(reportTO);
    }

    /**
     * Delete specified report.
     *
     * @param reportKey report to delete
     */
    public void delete(final String reportKey) {
        getService(ReportService.class).delete(reportKey);
    }

    @Override
    public void startExecution(final String reportKey, final Date startAt) {
        getService(ReportService.class).execute(new ExecSpecs.Builder().key(reportKey).
                startAt(DateOps.toOffsetDateTime(startAt)).build());
    }

    @Override
    public void deleteExecution(final String reportExecKey) {
        getService(ReportService.class).deleteExecution(reportExecKey);
    }

    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return getService(ReportService.class).listRecentExecutions(max);
    }

    public Optional<Response> exportExecutionResult(final String executionKey) {
        try {
            return Optional.of(getService(ReportService.class).exportExecutionResult(executionKey));
        } catch (SyncopeClientException e) {
            LOG.error("While exporting execution {}", executionKey, e);
            return Optional.empty();
        }
    }

    @Override
    public List<ExecTO> listExecutions(
            final String taskKey, final int page, final int size, final SortParam<String> sort) {

        return getService(ReportService.class).listExecutions(new ExecQuery.Builder().
                key(taskKey).page(page).size(size).orderBy(toOrderBy(sort)).build()).getResult();
    }

    @Override
    public long countExecutions(final String taskKey) {
        return getService(ReportService.class).
                listExecutions(new ExecQuery.Builder().key(taskKey).page(1).size(0).build()).getTotalCount();
    }

    @Override
    public Map<String, String> batch(final BatchRequest batchRequest) {
        List<BatchRequestItem> batchRequestItems = new ArrayList<>(batchRequest.getItems());

        Map<String, String> result = new LinkedHashMap<>();
        try {
            List<BatchResponseItem> batchResponseItems = batchRequest.commit().getItems();
            for (int i = 0; i < batchResponseItems.size(); i++) {
                String status = getStatus(batchResponseItems.get(i).getStatus());

                if (batchRequestItems.get(i).getRequestURI().contains("/execute")) {
                    result.put(StringUtils.substringAfterLast(
                            StringUtils.substringBefore(batchRequestItems.get(i).getRequestURI(), "/execute"), "/"),
                            status);
                } else {
                    result.put(StringUtils.substringAfterLast(
                            batchRequestItems.get(i).getRequestURI(), "/"), status);
                }
            }
        } catch (IOException e) {
            LOG.error("While processing Batch response", e);
        }

        return result;
    }
}
