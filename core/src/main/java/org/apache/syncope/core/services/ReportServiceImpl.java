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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.wrap.ReportletConfClass;
import org.apache.syncope.core.persistence.beans.ReportExec;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.rest.controller.ReportController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl extends AbstractServiceImpl implements ReportService {

    @Autowired
    private ReportController controller;

    @Override
    public Response create(final ReportTO reportTO) {
        ReportTO createdReportTO = controller.create(reportTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdReportTO.getId())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID.toString(), createdReportTO.getId()).
                build();
    }

    @Override
    public void update(final Long reportId, final ReportTO reportTO) {
        controller.update(reportTO);
    }

    @Override
    public PagedResult<ReportTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<ReportTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<ReportTO> list(final int page, final int size) {
        return list(page, size, null);
    }

    @Override
    public PagedResult<ReportTO> list(final int page, final int size, final String orderBy) {
        checkPageSize(page, size);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(controller.list(page, size, orderByClauses), page, size, controller.count());
    }

    @Override
    public List<ReportletConfClass> getReportletConfClasses() {
        return CollectionWrapper.wrap(controller.getReportletConfClasses(), ReportletConfClass.class);
    }

    @Override
    public ReportTO read(final Long reportId) {
        return controller.read(reportId);
    }

    @Override
    public ReportExecTO readExecution(final Long executionId) {
        return controller.readExecution(executionId);
    }

    @Override
    public Response exportExecutionResult(final Long executionId, final ReportExecExportFormat fmt) {
        final ReportExecExportFormat format = (fmt == null) ? ReportExecExportFormat.XML : fmt;
        final ReportExec reportExec = controller.getAndCheckReportExec(executionId);
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                controller.exportExecutionResult(os, reportExec, format);
            }
        };
        String disposition = "attachment; filename=" + reportExec.getReport().getName() + "." + format.name().
                toLowerCase();
        return Response.ok(sout).
                header(HttpHeaders.CONTENT_DISPOSITION, disposition).
                build();
    }

    @Override
    public ReportExecTO execute(final Long reportId) {
        return controller.execute(reportId);
    }

    @Override
    public void delete(final Long reportId) {
        controller.delete(reportId);
    }

    @Override
    public void deleteExecution(final Long executionId) {
        controller.deleteExecution(executionId);
    }
}
