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

import java.util.Date;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.service.ReportService;

public class ReportRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 1644689667998953604L;

    public ReportTO read(final String reportKey) {
        return getService(ReportService.class).read(reportKey);
    }

    public List<ReportTO> list() {
        return getService(ReportService.class).list();
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
}
