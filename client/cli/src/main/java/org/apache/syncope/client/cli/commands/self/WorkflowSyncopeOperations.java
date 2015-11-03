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
package org.apache.syncope.client.cli.commands.self;

import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.WorkflowService;

public class WorkflowSyncopeOperations {

    private final WorkflowService workflowService = SyncopeServices.get(WorkflowService.class);

    public Response exportDiagram(final String anyTypeKindString) {
        WebClient.client(workflowService).accept(RESTHeaders.MEDIATYPE_IMAGE_PNG);
        return workflowService.exportDiagram(AnyTypeKind.valueOf(anyTypeKindString));
    }

    public Response exportDefinition(final String anyTypeKindString) {
        return workflowService.exportDefinition(AnyTypeKind.valueOf(anyTypeKindString));
    }
}
