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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.RoleTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Path("/role")
@RequestMapping("/role")
public interface RoleService {

	@GET
    @Path("/{roleId}/children")
	@RequestMapping(method = RequestMethod.GET, value = "/children/{roleId}")
	List<RoleTO> children(@PathParam("roleId") @PathVariable("roleId") final Long roleId);

	@POST
    @Path("/")
	@RequestMapping(method = RequestMethod.POST, value = "/create")
	RoleTO create(@RequestBody final RoleTO roleTO);

	@DELETE
    @Path("/{roleId}")
	@RequestMapping(method = RequestMethod.GET, value = "/delete/{roleId}")
	RoleTO delete(@PathParam("roleId") @PathVariable("roleId") final Long roleId);

	@GET
	@RequestMapping(method = RequestMethod.GET, value = "/list")
	List<RoleTO> list();

	@GET
    @Path("/{roleId}/parent")
	@RequestMapping(method = RequestMethod.GET, value = "/parent/{roleId}")
	RoleTO parent(@PathParam("roleId") @PathVariable("roleId") final Long roleId);

	@GET
	@Path("/{roleId}")
	@RequestMapping(method = RequestMethod.GET, value = "/read/{roleId}")
	RoleTO read(@PathParam("roleId") @PathVariable("roleId") final Long roleId);

	
	@RequestMapping(method = RequestMethod.POST, value = "/search")
	List<RoleTO> search(@RequestBody final NodeCond searchCondition);

	@RequestMapping(method = RequestMethod.POST, value = "/search/{page}/{size}")
	List<RoleTO> search(@RequestBody final NodeCond searchCondition,
			@PathVariable("page") final int page,
			@PathVariable("size") final int size);

	@RequestMapping(method = RequestMethod.POST, value = "/search/count")
	int searchCount(@RequestBody final NodeCond searchCondition);

	/**
	 * @deprecated Authentication checks should not depend on the method called
	 */
	@Deprecated
	@RequestMapping(method = RequestMethod.GET, value = "/selfRead/{roleId}")
	RoleTO selfRead(@PathVariable("roleId") final Long roleId);

	@POST
    @Path("/{roleId}")
	@RequestMapping(method = RequestMethod.POST, value = "/update")
	RoleTO update(@PathParam("roleId") final Long roleId, @RequestBody final RoleMod roleMod);
}