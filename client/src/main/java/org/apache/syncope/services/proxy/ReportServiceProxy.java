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

import java.util.Arrays;
import java.util.List;

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
        return restTemplate.postForObject(baseUrl + "report/create", reportTO, ReportTO.class);
    }

    @Override
    public ReportTO update(Long reportId, ReportTO reportTO) {
        return restTemplate.postForObject(baseUrl + "report/update", reportTO, ReportTO.class);
    }

    @Override
    public int count() {
        return restTemplate.getForObject(baseUrl + "report/count.json", Integer.class);
    }

    @Override
    public List<ReportTO> list() {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "report/list", ReportTO[].class));
    }

    @Override
    public List<ReportTO> list(int page, int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ReportExecTO> listExecutions() {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "report/execution/list",
                ReportExecTO[].class));
    }

    @Override
    public List<String> getReportletConfClasses() {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "report/reportletConfClasses.json",
                String[].class));
    }

    @Override
    public ReportTO read(Long reportId) {
        return restTemplate.getForObject(baseUrl + "report/read/{reportId}", ReportTO.class, reportId);
    }

    @Override
    public ReportExecTO readExecution(Long executionId) {
        return restTemplate.getForObject(baseUrl + "report/execution/read/{reportId}",
                ReportExecTO.class, executionId);
    }

    @Override
    public void exportExecutionResult(Long executionId, ReportExecExportFormat fmt) {
        // TODO Auto-generated method stub

    }

    @Override
    public ReportExecTO execute(Long reportId) {
        return restTemplate.postForObject(baseUrl + "report/execute/{reportId}", null,
                ReportExecTO.class, reportId);
    }

    @Override
    public ReportTO delete(Long reportId) {
        return restTemplate.getForObject(baseUrl + "report/delete/{reportId}",
                ReportTO.class, reportId);
    }

    @Override
    public ReportExecTO deleteExecution(Long executionId) {
        // TODO Auto-generated method stub
        return null;
    }

}
