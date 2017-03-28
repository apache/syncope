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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class ReportRestClient extends BaseRestClient
        implements ExecutionRestClient, TemplateRestClient<ReportTemplateTO, ReportTemplateFormat> {

    private static final long serialVersionUID = 1644689667998953604L;

    public ReportTO read(final String reportKey) {
        return getService(ReportService.class).read(reportKey);
    }

    public List<ReportTO> list() {
        return getService(ReportService.class).list();
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
    public void startExecution(final String reportKey, final Date start) {
        getService(ReportService.class).execute(new ExecuteQuery.Builder().key(reportKey).startAt(start).build());
    }

    @Override
    public void deleteExecution(final String reportExecKey) {
        getService(ReportService.class).deleteExecution(reportExecKey);
    }

    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return getService(ReportService.class).listRecentExecutions(max);
    }

    public Response exportExecutionResult(final String executionKey, final ReportExecExportFormat fmt) {
        return getService(ReportService.class).exportExecutionResult(executionKey, fmt);
    }

    @Override
    public List<ExecTO> listExecutions(
            final String taskKey, final int page, final int size, final SortParam<String> sort) {

        return getService(ReportService.class).
                listExecutions(new ExecQuery.Builder().key(taskKey).page(page).size(size).
                        orderBy(toOrderBy(sort)).build()).getResult();
    }

    @Override
    public int countExecutions(final String taskKey) {
        return getService(ReportService.class).
                listExecutions(new ExecQuery.Builder().key(taskKey).page(1).size(1).build()).getTotalCount();
    }

    @Override
    public List<ReportTemplateTO> listTemplates() {
        return getService(ReportTemplateService.class).list();
    }

    @Override
    public void createTemplate(final ReportTemplateTO reportTemplateTO) {
        getService(ReportTemplateService.class).create(reportTemplateTO);
    }

    @Override
    public void deleteTemplate(final String key) {
        getService(ReportTemplateService.class).delete(key);
    }

    @Override
    public ReportTemplateTO readTemplate(final String key) {
        return getService(ReportTemplateService.class).read(key);
    }

    @Override
    public String readTemplateFormat(final String key, final ReportTemplateFormat format) {
        try {
            return IOUtils.toString(InputStream.class.cast(
                    getService(ReportTemplateService.class).getFormat(key, format).getEntity()),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Error retrieving mail template {} as {}", key, format, e);
            return StringUtils.EMPTY;
        }
    }

    @Override
    public void updateTemplateFormat(final String key, final String content, final ReportTemplateFormat format) {
        getService(ReportTemplateService.class).setFormat(
                key, format, IOUtils.toInputStream(content, StandardCharsets.UTF_8));
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        BulkActionResult result = new BulkActionResult();

        switch (action.getType()) {
            case DELETE:
                for (String target : action.getTargets()) {
                    delete(target);
                    result.getResults().put(target, BulkActionResult.Status.SUCCESS);
                }
                break;
            case EXECUTE:
                for (String target : action.getTargets()) {
                    startExecution(target, null);
                    result.getResults().put(target, BulkActionResult.Status.SUCCESS);
                }
                break;
            default:
                throw new NotSupportedException(action.getType().name());
        }
        return result;
    }
}
