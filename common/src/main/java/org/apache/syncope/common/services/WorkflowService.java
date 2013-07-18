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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.common.types.AttributableType;

@Path("workflows/{kind}")
public interface WorkflowService {

    /**
     * @param kind Kind can be USER or ROLE only!
     * @return Returns workflow definition for matching kind.
     */
    @GET
    WorkflowDefinitionTO getDefinition(@PathParam("kind") AttributableType kind);

    /**
     * @param kind Kind can be USER or ROLE only!
     * @param definition New workflow definition to be stored for matching kind.
     */
    @PUT
    void updateDefinition(@PathParam("kind") AttributableType kind, WorkflowDefinitionTO definition);

    /**
     * @param kind Kind can be USER or ROLE only!
     * @return Returns existing tasks for matching kind.
     */
    @GET
    @Path("tasks")
    WorkflowTasks getDefinedTasks(@PathParam("kind") AttributableType kind);
}
