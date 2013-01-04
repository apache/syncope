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
package org.apache.syncope.services;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.RoleTO;

@Path("roles")
public interface RoleService {

	@GET
    @Path("{roleId}/children")
	List<RoleTO> children(@PathParam("roleId") final Long roleId);

	@POST
	RoleTO create(final RoleTO roleTO);

	@DELETE
    @Path("{roleId}")
	RoleTO delete(@PathParam("roleId") final Long roleId);

	@GET
	List<RoleTO> list();

	@GET
    @Path("{roleId}/parent")
	RoleTO parent(@PathParam("roleId") final Long roleId);

	@GET
	@Path("{roleId}")
	RoleTO read(@PathParam("roleId") final Long roleId);

	@POST
	@Path("search")
	List<RoleTO> search(final NodeCond searchCondition);

	@POST
	@Path("search")
	List<RoleTO> search(final NodeCond searchCondition,
			@QueryParam("page") final int page,
			@QueryParam("size") @DefaultValue("25") final int size);

	@POST
	@Path("search/count")
	int searchCount(final NodeCond searchCondition);

	/**
	 * @deprecated Authentication checks should not depend on the method called
	 */
	@Deprecated
	RoleTO selfRead(final Long roleId);

	@POST
    @Path("{roleId}")
	RoleTO update(@PathParam("roleId") final Long roleId, final RoleMod roleMod);
}