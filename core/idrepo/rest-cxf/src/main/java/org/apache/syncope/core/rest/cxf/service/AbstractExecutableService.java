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

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.ExecutableService;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.logic.AbstractExecutableLogic;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.data.domain.Page;

public abstract class AbstractExecutableService extends AbstractService implements ExecutableService {

    protected abstract AbstractExecutableLogic<?> getExecutableLogic();

    @Override
    public PagedResult<ExecTO> listExecutions(final ExecQuery query) {
        Page<ExecTO> result = getExecutableLogic().listExecutions(
                query.getKey(),
                query.getBefore(),
                query.getAfter(),
                pageable(query));
        return buildPagedResult(result);
    }

    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return getExecutableLogic().listRecentExecutions(max);
    }

    @Override
    public void deleteExecution(final String executionKey) {
        getExecutableLogic().deleteExecution(executionKey);
    }

    @Override
    public Response deleteExecutions(final ExecQuery query) {
        List<BatchResponseItem> batchResponseItems = getExecutableLogic().deleteExecutions(
                query.getKey(),
                query.getBefore(),
                query.getAfter());

        String boundary = "deleteExecutions_" + SecureRandomUtils.generateRandomUUID().toString();
        return Response.ok(BatchPayloadGenerator.generate(
                batchResponseItems, JAXRSService.DOUBLE_DASH + boundary)).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();
    }

    @Override
    public ExecTO execute(final ExecSpecs execSpecs) {
        return getExecutableLogic().execute(execSpecs);
    }

    @Override
    public JobTO getJob(final String key) {
        return getExecutableLogic().getJob(key);
    }

    @Override
    public List<JobTO> listJobs() {
        return getExecutableLogic().listJobs();
    }

    @Override
    public void actionJob(final String key, final JobAction action) {
        getExecutableLogic().actionJob(key, action);
    }
}
