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
package org.apache.syncope.core.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.apache.syncope.core.persistence.beans.ReportExec;
import org.apache.syncope.core.persistence.dao.ReportDAO;
import org.apache.syncope.core.rest.controller.ReportController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService, ContextAware {

    @Autowired
    private ReportController reportController;

    @Autowired
    private ReportDAO reportDAO;

    private UriInfo uriInfo;

    @Override
    public Response create(final ReportTO reportTO) {
        ReportTO createdReportTO = reportController.createInternal(reportTO);
        URI location = uriInfo.getAbsolutePathBuilder().path("" + createdReportTO.getId()).build();
        return Response.created(location).build();
    }

    @Override
    public void update(final Long reportId, final ReportTO reportTO) {
        reportController.update(reportTO);
    }

    @Override
    public int count() {
        return reportDAO.count();
    }

    @Override
    public List<ReportTO> list() {
        return reportController.list();
    }

    @Override
    public List<ReportTO> list(final int page, final int size) {
        return reportController.list(page, size);
    }

    @Override
    public List<ReportExecTO> listExecutions() {
        return reportController.listExecutions();
    }

    @Override
    public Set<String> getReportletConfClasses() {
        return reportController.getReportletConfClassesInternal();
    }

    @Override
    public ReportTO read(final Long reportId) {
        return reportController.read(reportId);
    }

    @Override
    public ReportExecTO readExecution(final Long executionId) {
        return reportController.readExecution(executionId);
    }

    @Override
    public Response exportExecutionResult(final Long executionId, final ReportExecExportFormat fmt) {
        final ReportExecExportFormat format = (fmt == null) ? ReportExecExportFormat.XML : fmt;
        final ReportExec reportExec = reportController.getAndCheckReportExecInternal(executionId);
        return Response.ok(new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException {
                reportController.exportExecutionResultInternal(os, reportExec, format);
            }
        }).build();
    }

    @Override
    public ReportExecTO execute(final Long reportId) {
        return reportController.execute(reportId);
    }

    @Override
    public void delete(final Long reportId) {
        reportController.delete(reportId);
    }

    @Override
    public void deleteExecution(final Long executionId) {
        reportController.deleteExecution(executionId);
    }

    @Override
    public void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

}
