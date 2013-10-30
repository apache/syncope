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
package org.apache.syncope.common.services;

import java.util.List;
import javax.ws.rs.Consumes;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;

import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.types.TaskType;

@Path("tasks")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface TaskService {

    /**
     * @param taskType filter for task count
     * @return Returns number of tasks with matching type
     */
    @GET
    @Path("count")
    int count(@MatrixParam("type") TaskType taskType);

    /**
     * @param taskTO Task to be created
     * @param <T> type of taskTO
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created task
     */
    @POST
    <T extends SchedTaskTO> Response create(T taskTO);

    /**
     * @param taskId Id of task to be deleted
     */
    @DELETE
    @Path("{taskId}")
    void delete(@PathParam("taskId") Long taskId);

    /**
     * @param executionId ID of task execution to be deleted
     */
    @DELETE
    @Path("executions/{executionId}")
    void deleteExecution(@PathParam("executionId") Long executionId);

    /**
     * @param taskId Id of task to be executed
     * @param dryRun if true, task will only be simulated
     * @return Returns TaskExcecution
     */
    @POST
    @Path("{taskId}/execute")
    TaskExecTO execute(@PathParam("taskId") Long taskId, @QueryParam("dryRun") @DefaultValue("false") boolean dryRun);

    /**
     * @return Returns list of JobClasses
     */
    @GET
    @Path("jobClasses")
    List<JobClassTO> getJobClasses();

    /**
     * @return Returns list of SyncActionClasses
     */
    @GET
    @Path("syncActionsClasses")
    List<SyncActionClassTO> getSyncActionsClasses();

    /**
     * @param taskType Type of tasks to be listed
     * @param <T> type of taskTO
     * @return Returns list of tasks with matching type
     */
    @GET
    <T extends AbstractTaskTO> List<T> list(@MatrixParam("type") TaskType taskType);

    /**
     * @param taskType Type of tasks to be listed
     * @param page Page number of tasks in relation to page size
     * @param size Number of tasks listed per page
     * @param <T> type of taskTO
     * @return Returns paginated list of task with matching type
     */
    @GET
    <T extends AbstractTaskTO> List<T> list(@MatrixParam("type") TaskType taskType,
            @QueryParam("page") int page, @QueryParam("size") @DefaultValue("25") int size);

    /**
     * @param taskId Id of task to be read
     * @param <T> type of taskTO
     * @return Returns task with matching id
     */
    @GET
    @Path("{taskId}")
    <T extends AbstractTaskTO> T read(@PathParam("taskId") Long taskId);

    /**
     * @param executionId Id if task execution to be read
     * @return Returns task execution with matching Id
     */
    @GET
    @Path("executions/{executionId}")
    TaskExecTO readExecution(@PathParam("executionId") Long executionId);

    /**
     * @param executionId Task execution ID related to report
     * @param report Report for task execution
     */
    @POST
    @Path("executions/{executionId}/report")
    void report(@PathParam("executionId") Long executionId, ReportExecTO report);

    /**
     * @param taskId Id if task to be updated
     * @param taskTO New task to be stored
     */
    @PUT
    @Path("{taskId}")
    void update(@PathParam("taskId") Long taskId, AbstractTaskTO taskTO);

    @POST
    @Path("bulk")
    BulkActionRes bulk(BulkAction bulkAction);
}
