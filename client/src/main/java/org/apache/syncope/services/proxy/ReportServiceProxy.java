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
package org.apache.syncope.services.proxy;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.client.to.ReportExecTO;
import org.apache.syncope.client.to.ReportTO;
import org.apache.syncope.services.ReportService;
import org.apache.syncope.types.ReportExecExportFormat;
import org.springframework.web.client.RestTemplate;

public class ReportServiceProxy extends SpringServiceProxy implements ReportService {

    public ReportServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public ReportTO create(ReportTO reportTO) {
        return getRestTemplate().postForObject(baseUrl + "report/create", reportTO, ReportTO.class);
    }

    @Override
    public ReportTO update(Long reportId, ReportTO reportTO) {
        return getRestTemplate().postForObject(baseUrl + "report/update", reportTO, ReportTO.class);
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
    public List<ReportTO> list(int page, int size) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "report/list/{page}/{size}", ReportTO[].class,
                page, size));
    }

    @Override
    public List<ReportExecTO> listExecutions() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "report/execution/list", ReportExecTO[].class));
    }

    @Override
    public List<String> getReportletConfClasses() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "report/reportletConfClasses.json",
                String[].class));
    }

    @Override
    public ReportTO read(Long reportId) {
        return getRestTemplate().getForObject(baseUrl + "report/read/{reportId}", ReportTO.class, reportId);
    }

    @Override
    public ReportExecTO readExecution(Long executionId) {
        return getRestTemplate().getForObject(baseUrl + "report/execution/read/{executionId}", ReportExecTO.class,
                executionId);
    }

    @Override
    public Response exportExecutionResult(Long executionId, ReportExecExportFormat fmt) {
        String format = (fmt != null)
                ? "?fmt=" + fmt.toString()
                : "";
        InputStream stream = getRestTemplate().getForObject(baseUrl + "report/execution/export/{executionId}" + format,
                InputStream.class, executionId);
        return Response.ok(stream).build();
    }

    @Override
    public ReportExecTO execute(Long reportId) {
        return getRestTemplate().postForObject(baseUrl + "report/execute/{reportId}", null, ReportExecTO.class,
                reportId);
    }

    @Override
    public ReportTO delete(Long reportId) {
        return getRestTemplate().getForObject(baseUrl + "report/delete/{reportId}", ReportTO.class, reportId);
    }

    @Override
    public ReportExecTO deleteExecution(Long executionId) {
        return getRestTemplate().getForObject(baseUrl + "report/execution/delete/{executionId}", ReportExecTO.class,
                executionId);
    }

}
