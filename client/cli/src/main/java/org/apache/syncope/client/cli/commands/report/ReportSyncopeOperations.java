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
package org.apache.syncope.client.cli.commands.report;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.service.ReportService;

public class ReportSyncopeOperations {

    private final ReportService reportService = SyncopeServices.get(ReportService.class);

    public ReportExecTO readExecution(final String executionid) {
        return reportService.readExecution(Long.valueOf(executionid));
    }

    public ReportTO read(final String reportId) {
        return reportService.read(Long.valueOf(reportId));
    }

    public List<ReportExecTO> listJobs(final JobStatusType jobStatusType) {
        return reportService.listJobs(jobStatusType);
    }

    public List<ReportTO> list() {
        return reportService.list();
    }

    public Response exportExecutionResult(final String executionKey, final ReportExecExportFormat fmt) {
        return reportService.exportExecutionResult(Long.valueOf(executionKey), fmt);
    }

    public void execute(final String reportId) {
        reportService.execute(Long.valueOf(reportId));
    }

    public void deleteExecution(final String executionId) {
        reportService.deleteExecution(Long.valueOf(executionId));
    }

    public void delete(final String reportId) {
        reportService.delete(Long.valueOf(reportId));
    }
}
