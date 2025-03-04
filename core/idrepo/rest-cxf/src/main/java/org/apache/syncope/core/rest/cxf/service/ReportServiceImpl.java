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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.core.logic.AbstractExecutableLogic;
import org.apache.syncope.core.logic.ReportLogic;

public class ReportServiceImpl extends AbstractExecutableService implements ReportService {

    protected final ReportLogic logic;

    public ReportServiceImpl(final ReportLogic logic) {
        this.logic = logic;
    }

    @Override
    protected AbstractExecutableLogic<?> getExecutableLogic() {
        return logic;
    }

    @Override
    public Response create(final ReportTO reportTO) {
        ReportTO createdReportTO = logic.create(reportTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdReportTO.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, createdReportTO.getKey()).
                build();
    }

    @Override
    public void update(final ReportTO reportTO) {
        logic.update(reportTO);
    }

    @Override
    public List<ReportTO> list() {
        return logic.list();
    }

    @Override
    public ReportTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response exportExecutionResult(final String executionKey) {
        String filename = logic.getFilename(executionKey);
        StreamingOutput sout = os -> logic.exportExecutionResult(os, executionKey);

        return Response.ok(sout).
                header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).
                build();
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
