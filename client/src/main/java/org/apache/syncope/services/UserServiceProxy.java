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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.to.WorkflowFormTO;
import org.springframework.web.client.RestTemplate;

public class UserServiceProxy extends SpringServiceProxy implements UserService {

	public UserServiceProxy(String baseUrl, RestTemplate restTemplate) {
		super(baseUrl, restTemplate);
	}

	@Override
	public Boolean verifyPassword(@MatrixParam("uname") String username,
			@MatrixParam("pwd") String password) {
		return restTemplate.
                getForObject(BASE_URL + "user/verifyPassword/{username}.json?password={password}",
                Boolean.class, username, password);
	}

	@Override
	public int count() {
		return restTemplate.getForObject(BASE_URL + "user/count.json", Integer.class);
	}

	@Override
	public List<UserTO> list() {
		return  Arrays.asList(restTemplate.getForObject(BASE_URL + "user/list.json", UserTO[].class));
	}

	@Override
	public List<UserTO> list(@QueryParam("page") int page,
			@QueryParam("size") @DefaultValue("25") int size) {
		return Arrays.asList(restTemplate.getForObject(BASE_URL + "user/list/{page}/{size}.json",
                UserTO[].class, page, size));
	}

	@Override
	public UserTO read(@PathParam("userId") Long userId) {
		return restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userId);
	}

	@Override
	public UserTO read(@MatrixParam("uname") String username) {
		return null;
	}

	@Override
	@POST
	@Path("/")
	public UserTO create(UserTO userTO) {
		return restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
	}

	@Override
	public UserTO update(@PathParam("userId") Long userId, UserMod userMod) {
		return restTemplate.postForObject(BASE_URL + "user/update", userMod, UserTO.class);
	}

	@Override
	public UserTO delete(@PathParam("userId") Long userId) {
		return restTemplate.getForObject(BASE_URL + "user/delete/{userId}", UserTO.class, userId);
	}

	@Override
	public UserTO executeWorkflow(@PathParam("taskId") String taskId, UserTO userTO) {
		return null;
	}

	@Override
	@GET
	@Path("/workflow/form")
	public List<WorkflowFormTO> getForms() {
		return  Arrays.asList(restTemplate.getForObject(BASE_URL + "user/workflow/form/list", WorkflowFormTO[].class));
	}

	@Override
	public WorkflowFormTO getFormForUser(@PathParam("userId") Long userId) {
		return restTemplate.getForObject(BASE_URL + "user/workflow/form/{userId}", WorkflowFormTO.class, userId);
	}

	@Override
	public WorkflowFormTO claimForm(@PathParam("taskId") String taskId) {
		return restTemplate.getForObject(BASE_URL + "user/workflow/form/claim/{taskId}", WorkflowFormTO.class, taskId);
	}

	@Override
	public UserTO submitForm(WorkflowFormTO form) {
		return restTemplate.postForObject(BASE_URL + "user/workflow/form/submit", form, UserTO.class);
	}

	@Override
	public UserTO activate(long userId, String token) {
		return restTemplate.getForObject(BASE_URL + "user/activate/{userId}?token=" + token, UserTO.class, userId);
	}

	@Override
	public UserTO activateByUsername(String username, String token) {
		return restTemplate.getForObject(BASE_URL + "user/activateByUsername/{username}.json?token=" + token,
                UserTO.class, username);
	}

	@Override
	public UserTO suspend(long userId) {
		return restTemplate.getForObject(BASE_URL + "user/suspend/{userId}", UserTO.class, userId);
	}

	@Override
	public UserTO reactivate(long userId) {
		return restTemplate.getForObject(BASE_URL + "user/reactivate/{userId}", UserTO.class, userId);
	}
	
	@Override
	public UserTO reactivate(long userId, String query) {
		return restTemplate.getForObject(BASE_URL + "user/reactivate/" + userId + query, UserTO.class);
	}

	@Override
	public UserTO suspendByUsername(String username) {
		return restTemplate.getForObject(BASE_URL + "user/suspendByUsername/{username}.json", UserTO.class, username);
	}

	@Override
	public UserTO reactivateByUsername(String username) {
		return restTemplate.getForObject(BASE_URL + "user/reactivateByUsername/{username}.json", UserTO.class, username);
	}

	@Override
	public UserTO suspend(long userId, String query) {
		return  restTemplate.getForObject(BASE_URL + "user/suspend/" + userId + query, UserTO.class);
	}

	@Override
	public UserTO readSelf() {
		return restTemplate.getForObject(BASE_URL + "user/read/self", UserTO.class);
	}

	@Override
	public List<UserTO> search(NodeCond searchCondition) {
		return Arrays.asList(restTemplate.postForObject(BASE_URL + "user/search", searchCondition,
                UserTO[].class));
	}

	@Override
	public List<UserTO> search(NodeCond searchCondition, int page, int size) {
		return Arrays.asList(restTemplate.postForObject(BASE_URL + "user/search/{page}/{size}",
                searchCondition, UserTO[].class, page, size));
	}

	@Override
	public int searchCount(NodeCond searchCondition) {
		return restTemplate.postForObject(BASE_URL + "user/search/count.json", searchCondition, Integer.class);
	}

}
