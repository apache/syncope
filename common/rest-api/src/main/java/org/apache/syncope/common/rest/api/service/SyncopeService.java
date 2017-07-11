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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;

/**
 * General info about this Apache Syncope deployment.
 */
@Path("")
public interface SyncopeService extends JAXRSService {

    /**
     * Provides information summary about platform configuration (workflow adapters, provisioning managers, validators,
     * actions, correlation rules, reportlets, ...).
     *
     * @return information summary about platform configuration (workflow adapters, provisioning managers, validators,
     * actions, correlation rules, reportlets, ...)
     */
    @GET
    @Path("/platform")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PlatformInfo platform();

    /**
     * Provides information about the underlying system (Operating System, CPU / memory usage, ...).
     *
     * @return information about the underlying system (Operating System, CPU / memory usage, ...)
     */
    @GET
    @Path("/system")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    SystemInfo system();

    /** *
     * Provides some numbers about the managed entities (users, groups, any objects...).
     *
     * @return some numbers about the managed entities (users, groups, any objects...)
     */
    @GET
    @Path("/numbers")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    NumbersInfo numbers();

    /**
     * Returns the list of Groups, according to provided paging instructions, assignable to Users and Any Objects of
     * the provided Realm.
     *
     * @param realm of the User and Any Objects assignable to the returned Groups
     * @param page search page
     * @param size search page size
     * @return list of Groups, according to provided paging instructions, assignable to Users and Any Objects of
     * the provided Realm
     */
    @POST
    @Path("/assignableGroups/{realm:.*}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedResult<GroupTO> searchAssignableGroups(
            @NotNull @PathParam("realm") String realm,
            @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue("1") int page,
            @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue("25") int size);

    /**
     * Extracts User type extension information, for the provided group.
     *
     * @param groupName group name
     * @return User type extension information, for the provided group
     */
    @GET
    @Path("/userTypeExtension/{groupName}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    TypeExtensionTO readUserTypeExtension(
            @NotNull @PathParam("groupName") String groupName);
}
