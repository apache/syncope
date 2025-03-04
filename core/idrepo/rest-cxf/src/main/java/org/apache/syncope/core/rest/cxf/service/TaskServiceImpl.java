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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.logic.AbstractExecutableLogic;
import org.apache.syncope.core.logic.TaskLogic;
import org.springframework.data.domain.Page;
import org.springframework.util.CollectionUtils;

public class TaskServiceImpl extends AbstractExecutableService implements TaskService {

    protected final TaskLogic logic;

    public TaskServiceImpl(final TaskLogic logic) {
        this.logic = logic;
    }

    @Override
    protected AbstractExecutableLogic<?> getExecutableLogic() {
        return logic;
    }

    @Override
    public Response create(final TaskType type, final SchedTaskTO taskTO) {
        SchedTaskTO createdTask;
        if (taskTO != null) {
            createdTask = logic.createSchedTask(type, taskTO);
        } else {
            throw new BadRequestException();
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(createdTask.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, createdTask.getKey()).
                build();
    }

    @Override
    public void delete(final TaskType type, final String key) {
        logic.delete(type, key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TaskTO> PagedResult<T> search(final TaskQuery query) {
        Page<T> result = logic.search(
                query.getType(),
                query.getResource(),
                query.getNotification(),
                query.getAnyTypeKind(),
                query.getEntityKey(),
                pageable(query),
                query.getDetails());
        return buildPagedResult(result);
    }

    @Override
    public <T extends TaskTO> T read(final TaskType type, final String key, final boolean details) {
        return logic.read(type, key, details);
    }

    @Override
    public void update(final TaskType type, final SchedTaskTO taskTO) {
        logic.updateSchedTask(type, taskTO);
    }

    @Override
    public Response purgePropagations(
            final OffsetDateTime since,
            final List<ExecStatus> statuses,
            final List<String> resources) {

        if (since == null && CollectionUtils.isEmpty(statuses) && CollectionUtils.isEmpty(resources)) {
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }

        return Response.ok(logic.purgePropagations(since, statuses, resources)).build();
    }

    @Override
    public SyncopeForm getMacroTaskForm(final String key, final String locale) {
        Locale localeObj = null;
        try {
            localeObj = LocaleUtils.toLocale(locale);
        } catch (Exception e) {
            LOG.error("While attempting to convert {} to Locale", locale, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            sce.getElements().add("Invalid locale '" + locale + "'");
            throw sce;
        }

        return logic.getMacroTaskForm(key, localeObj);
    }

    @Override
    public ExecTO execute(final ExecSpecs specs, final SyncopeForm macroTaskForm) {
        return logic.execute(specs, macroTaskForm);
    }
}
