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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;

@Path("userworkflow")
public interface UserWorkflowService {
	@POST
	@Path("forms")
	UserTO submitForm(WorkflowFormTO form);

	@GET
	@Path("forms")
	List<WorkflowFormTO> getForms();

	@GET
	@Path("forms/{userId}")
	WorkflowFormTO getFormForUser(@PathParam("userId") Long userId);
	
	@POST
	@Path("tasks/{taskId}/claim")
	WorkflowFormTO claimForm(@PathParam("taskId") String taskId);

	@POST
	@Path("tasks/{taskId}/execute")
	UserTO executeWorkflow(@PathParam("taskId") String taskId, UserTO userTO);
}
