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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ReportletConfClasses;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.springframework.stereotype.Component;

@Component
public class ReportRestClient extends BaseRestClient implements ExecutionRestClient {

    private static final long serialVersionUID = 1644689667998953604L;

    public List<String> getReportletConfClasses() {
        try {
            ReportletConfClasses reportletConfClasses = getService(ReportService.class).getReportletConfClasses();
            return reportletConfClasses.getConfClasses();
        } catch (SyncopeClientCompositeException e) {
            LOG.error("While getting available reportlet classes", e);
            return new ArrayList<String>();
        }
    }

    public ReportTO read(final Long reportId) {
        return getService(ReportService.class).read(reportId);
    }

    public List<ReportTO> list() {
        return getService(ReportService.class).list();
    }

    public List<ReportTO> list(final int page, final int size) {
        return getService(ReportService.class).list(page, size);
    }

    public int count() {
        return getService(ReportService.class).count();
    }

    public void create(final ReportTO reportTO) {
        getService(ReportService.class).create(reportTO);
    }

    public void update(final ReportTO reportTO) {
        getService(ReportService.class).update(reportTO.getId(), reportTO);
    }

    /**
     * Delete specified report.
     *
     * @param reportId report to delete
     */
    public void delete(final Long reportId) {
        getService(ReportService.class).delete(reportId);
    }

    /**
     * Start execution for the specified report.
     *
     * @param reportId report id
     */
    @Override
    public void startExecution(final long reportId) {
        getService(ReportService.class).execute(reportId);
    }

    /**
     * Delete specified report execution.
     *
     * @param reportExecId report execution id
     */
    @Override
    public void deleteExecution(final long reportExecId) {
        getService(ReportService.class).deleteExecution(reportExecId);
    }

    public Response exportExecutionResult(final Long executionId, final ReportExecExportFormat fmt) {
        return getService(ReportService.class).exportExecutionResult(executionId, fmt);
    }
}
