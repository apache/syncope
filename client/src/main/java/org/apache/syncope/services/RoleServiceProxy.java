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

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.RoleTO;
import org.springframework.web.client.RestTemplate;

public class RoleServiceProxy extends SpringServiceProxy implements RoleService {

	public RoleServiceProxy(String baseUrl, RestTemplate restTemplate) {
		super(baseUrl, restTemplate);
	}

	@Override
	public List<RoleTO> children(Long roleId) {
		return Arrays.asList(restTemplate.getForObject(baseUrl
				+ "role/children/{roleId}.json", RoleTO[].class, roleId));
	}

	@Override
	public RoleTO create(RoleTO roleTO) {
		return restTemplate.postForObject(baseUrl + "role/create", roleTO,
				RoleTO.class);
	}

	@Override
	public RoleTO delete(Long roleId) {
		return restTemplate.getForObject(baseUrl + "role/delete/{roleId}",
				RoleTO.class, roleId);
	}

	@Override
	public List<RoleTO> list() {
		return Arrays.asList(restTemplate.getForObject(baseUrl
				+ "role/list.json", RoleTO[].class));
	}

	@Override
	public RoleTO parent(Long roleId) {
		return restTemplate.getForObject(baseUrl + "role/parent/{roleId}.json",
				RoleTO.class, roleId);
	}

	@Override
	public RoleTO read(Long roleId) {
		return restTemplate.getForObject(baseUrl + "role/read/{roleId}.json",
				RoleTO.class, roleId);
	}

	@Override
	public List<RoleTO> search(NodeCond searchCondition) {
		return Arrays.asList(restTemplate.postForObject(
				baseUrl + "role/search", searchCondition, RoleTO[].class));
	}

	@Override
	public List<RoleTO> search(NodeCond searchCondition, int page, int size) {
		return Arrays.asList(restTemplate.postForObject(
				baseUrl + "role/search/{page}/{size}", searchCondition, RoleTO[].class, page, size));
	}

	@Override
	public int searchCount(NodeCond searchCondition) {
		return restTemplate.postForObject(baseUrl + "role/search/count.json",
				searchCondition, Integer.class);
	}

	@Override
	public RoleTO selfRead(Long roleId) {
		return restTemplate.getForObject(baseUrl + "role/selfRead/{roleId}",
				RoleTO.class, roleId);
	}

	@Override
	public RoleTO update(Long roleId, RoleMod roleMod) {
		return restTemplate.postForObject(baseUrl + "role/update", roleMod,
				RoleTO.class);
	}

}
