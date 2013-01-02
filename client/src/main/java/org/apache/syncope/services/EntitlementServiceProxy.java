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
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.client.RestTemplate;

public class EntitlementServiceProxy extends SpringServiceProxy implements EntitlementService {
	
	public EntitlementServiceProxy(String baseUrl, RestTemplate restTemplate) {
		super(baseUrl, restTemplate);
	}

	@Override
	public Set<String> getAllEntitlements() {
		return new HashSet<String>(Arrays.asList(new RestTemplate().getForObject(
                baseUrl + "auth/allentitlements.json", String[].class)));
	}

	@Override
	public Set<String> getMyEntitlements() {
		return new HashSet<String>(Arrays.asList(restTemplate.getForObject(baseUrl
                + "auth/entitlements.json", String[].class)));
	}

}
