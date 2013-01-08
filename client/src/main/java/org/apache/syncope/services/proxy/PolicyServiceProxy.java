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
package org.apache.syncope.services.proxy;

import java.util.List;

import org.apache.syncope.client.to.PolicyTO;
import org.apache.syncope.services.PolicyService;
import org.apache.syncope.types.PolicyType;
import org.springframework.web.client.RestTemplate;

public class PolicyServiceProxy extends SpringServiceProxy implements PolicyService {

	public PolicyServiceProxy(String baseUrl, RestTemplate restTemplate) {
		super(baseUrl, restTemplate);
	}

	@Override
	public <T extends PolicyTO> T create(final T policyTO) {
		@SuppressWarnings("unchecked")
		T result = (T) restTemplate.postForObject(BASE_URL
				+ "policy/{kind}/create", policyTO, policyTO.getClass(),
				typeToUrl(policyTO.getType()));
		return result;
	}

	@Override
	public <T extends PolicyTO> T update(Long policyId, T policyTO) {
		@SuppressWarnings("unchecked")
		T result = (T) restTemplate.postForObject(BASE_URL
				+ "policy/{kind}/update", policyTO, policyTO.getClass(),
				typeToUrl(policyTO.getType()));
		return result;
	}

	@Override
	public <T extends PolicyTO> List<T> listByType(PolicyType type) {
		@SuppressWarnings("unchecked")
		List<T> result = restTemplate.getForObject(BASE_URL + "policy/{kind}/list",
				List.class, typeToUrl(type));
		return result;
	}

	@Override
	public <T extends PolicyTO> T readGlobal(PolicyType type, Class<T> policyClass) {
		T result = restTemplate.getForObject(BASE_URL + "policy/{kind}/global/read",
                policyClass, typeToUrl(type));
		return result;
	}

	@Override
	public <T extends PolicyTO> T read(Long policyId, Class<T> policyClass) {
		T result = restTemplate.getForObject(BASE_URL + "policy/read/{id}", policyClass, policyId);
		return result;
	}

	@Override
	public <T extends PolicyTO> T delete(Long policyId, Class<T> policyClass) {
		T result = restTemplate.getForObject(BASE_URL + "policy/delete/{id}", policyClass, policyId);
		return result;
	}

    private String typeToUrl(PolicyType type) {
    	String url = type.name().toLowerCase();
    	int index = url.indexOf("_");
    	if (index != -1) {
    		return url.substring(index + 1);
    	} else {
    		return url;
    	}
    }
}
