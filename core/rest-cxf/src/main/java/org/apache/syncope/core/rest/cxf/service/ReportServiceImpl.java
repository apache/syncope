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
package org.apache.syncope.core.rest.cxf.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.wrap.ReportletConfClass;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ListQuery;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl extends AbstractServiceImpl implements ReportService {

    @Autowired
    private ReportLogic logic;

    @Override
    public Response create(final ReportTO reportTO) {
        ReportTO createdReportTO = logic.create(reportTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdReportTO.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, createdReportTO.getKey()).
                build();
    }

    @Override
    public void update(final Long reportKey, final ReportTO reportTO) {
        reportTO.setKey(reportKey);
        logic.update(reportTO);
    }

    @Override
    public PagedResult<ReportTO> list(final ListQuery listQuery) {
        return buildPagedResult(
                logic.list(
                        listQuery.getPage(),
                        listQuery.getSize(),
                        getOrderByClauses(listQuery.getOrderBy())),
                listQuery.getPage(),
                listQuery.getSize(),
                logic.count());
    }

    @Override
    public List<ReportletConfClass> getReportletConfClasses() {
        return CollectionWrapper.wrap(logic.getReportletConfClasses(), ReportletConfClass.class);
    }

    @Override
    public ReportTO read(final Long reportKey) {
        return logic.read(reportKey);
    }

    @Override
    public ReportExecTO readExecution(final Long executionKey) {
        return logic.readExecution(executionKey);
    }

    @Override
    public Response exportExecutionResult(final Long executionKey, final ReportExecExportFormat fmt) {
        final ReportExecExportFormat format = (fmt == null) ? ReportExecExportFormat.XML : fmt;
        final ReportExec reportExec = logic.getAndCheckReportExec(executionKey);
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                logic.exportExecutionResult(os, reportExec, format);
            }
        };
        String disposition = "attachment; filename=" + reportExec.getReport().getName() + "." + format.name().
                toLowerCase();
        return Response.ok(sout).
                header(HttpHeaders.CONTENT_DISPOSITION, disposition).
                build();
    }

    @Override
    public ReportExecTO execute(final Long reportKey) {
        return logic.execute(reportKey);
    }

    @Override
    public void delete(final Long reportKey) {
        logic.delete(reportKey);
    }

    @Override
    public void deleteExecution(final Long executionKey) {
        logic.deleteExecution(executionKey);
    }

    @Override
    public List<ReportExecTO> list(final JobStatusType type) {
        return logic.list(type, ReportExecTO.class);
    }

    @Override
    public void process(final JobAction action, final Long reportId) {
        logic.process(action, reportId);
    }
}
