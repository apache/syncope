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
package org.apache.syncope.client.services.proxy;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ReportletConfClasses;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.springframework.web.client.RestTemplate;

public class ReportServiceProxy extends SpringServiceProxy implements ReportService {

    public ReportServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Response create(final ReportTO reportTO) {
        ReportTO createdReportTO = getRestTemplate().postForObject(baseUrl + "report/create", reportTO, ReportTO.class);
        URI location = URI.create(baseUrl + "report/read/" + createdReportTO.getId() + ".json");
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, createdReportTO.getId()).build();
    }

    @Override
    public void update(final Long reportId, final ReportTO reportTO) {
        getRestTemplate().postForObject(baseUrl + "report/update", reportTO, ReportTO.class);
    }

    @Override
    public int count() {
        return getRestTemplate().getForObject(baseUrl + "report/count.json", Integer.class);
    }

    @Override
    public List<ReportTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "report/list", ReportTO[].class));
    }

    @Override
    public List<ReportTO> list(final int page, final int size) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "report/list/{page}/{size}", ReportTO[].class,
                page, size));
    }

    @Override
    public ReportletConfClasses getReportletConfClasses() {
        List<String> confClasses = Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "report/reportletConfClasses.json", String[].class));
        return new ReportletConfClasses(confClasses);
    }

    @Override
    public ReportTO read(final Long reportId) {
        return getRestTemplate().getForObject(baseUrl + "report/read/{reportId}", ReportTO.class, reportId);
    }

    @Override
    public ReportExecTO readExecution(final Long executionId) {
        return getRestTemplate().getForObject(baseUrl + "report/execution/read/{executionId}", ReportExecTO.class,
                executionId);
    }

    @Override
    public Response exportExecutionResult(final Long executionId, final ReportExecExportFormat fmt) {
        final String format = fmt == null
                ? ""
                : "?fmt=" + fmt.toString();
        return handleStream(baseUrl + "report/execution/export/" + executionId + format);
    }

    @Override
    public ReportExecTO execute(final Long reportId) {
        return getRestTemplate().postForObject(baseUrl + "report/execute/{reportId}", null, ReportExecTO.class,
                reportId);
    }

    @Override
    public void delete(final Long reportId) {
        getRestTemplate().getForObject(baseUrl + "report/delete/{reportId}", ReportTO.class, reportId);
    }

    @Override
    public void deleteExecution(final Long executionId) {
        getRestTemplate().getForObject(baseUrl + "report/execution/delete/{executionId}", ReportExecTO.class,
                executionId);
    }
}
