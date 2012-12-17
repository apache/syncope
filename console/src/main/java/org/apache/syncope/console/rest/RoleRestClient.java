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
package org.apache.syncope.console.rest;

import java.util.List;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.services.RoleService;
import org.apache.syncope.services.UnauthorizedRoleException;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractBaseRestClient {

	private RoleService rs = super.getRestService(RoleService.class);

	/**
	 * Get all Roles.
	 * 
	 * @return SchemaTOs
	 */
	public List<RoleTO> getAllRoles()
			throws SyncopeClientCompositeErrorException {
		List<RoleTO> roles = null;

		try {
			// roles =
			// Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
			// baseURL + "role/list.json", RoleTO[].class));
			roles = rs.list();
		} catch (SyncopeClientCompositeErrorException e) {
			LOG.error("While listing all roles", e);
		}

		return roles;
	}

	public void create(final RoleTO roleTO) {
		// SyncopeSession.get().getRestTemplate().postForObject(
		// baseURL + "role/create", roleTO, RoleTO.class);
		rs.create(roleTO);
	}

	public RoleTO read(final Long id) {
		RoleTO roleTO = null;

		try {
			// roleTO = SyncopeSession.get().getRestTemplate().getForObject(
			// baseURL + "role/read/{roleId}.json", RoleTO.class, id);
			roleTO = rs.read(id);
			// } catch (SyncopeClientCompositeErrorException e) {
		} catch (Exception e) {
			LOG.error("While reading a role", e);
		}
		return roleTO;
	}

	public void update(final RoleMod roleMod) throws UnauthorizedRoleException,
			NotFoundException {
		// SyncopeSession.get().getRestTemplate().postForObject(
		// baseURL + "role/update", roleMod, RoleTO.class);
		rs.update(roleMod.getId(), roleMod);
	}

	public RoleTO delete(final Long id) throws UnauthorizedRoleException {
//		return SyncopeSession
//				.get()
//				.getRestTemplate()
//				.getForObject(baseURL + "role/delete/{roleId}.json",
//						RoleTO.class, id);
		RoleTO role = read(id);
		try {
			rs.delete(id);
		} catch (NotFoundException e) {
			LOG.warn("Deletion of unexisting role", e);
		}
		return role;
	}
}
