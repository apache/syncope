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
package org.apache.syncope.common.rest.api.service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.rest.api.beans.TaskQuery;

/**
 * REST operations for tasks.
 */
@Path("tasks")
public interface TaskService extends ExecutableService {

    /**
     * Returns the task matching the given key.
     *
     * @param key key of task to be read
     * @param details whether include executions or not, defaults to true
     * @param <T> type of taskTO
     * @return task with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractTaskTO> T read(
            @NotNull @PathParam("key") String key,
            @QueryParam(JAXRSService.PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Returns a paged list of existing tasks matching the given query.
     *
     * @param query query conditions
     * @param <T> type of taskTO
     * @return paged list of existing tasks matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractTaskTO> PagedResult<T> list(@BeanParam TaskQuery query);

    /**
     * Creates a new task.
     *
     * @param taskTO task to be created
     * @return Response object featuring Location header of created task
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull SchedTaskTO taskTO);

    /**
     * Updates the task matching the provided key.
     *
     * @param taskTO updated task to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull AbstractTaskTO taskTO);

    /**
     * Deletes the task matching the provided key.
     *
     * @param key key of task to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of task ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}
