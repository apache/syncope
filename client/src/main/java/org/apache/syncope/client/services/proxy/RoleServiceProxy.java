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
package org.apache.syncope.client.services.proxy;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.to.RoleTO;
import org.springframework.web.client.RestTemplate;

public class RoleServiceProxy extends SpringServiceProxy implements RoleService {

    public RoleServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public List<RoleTO> children(final Long roleId) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "role/children/{roleId}.json",
                RoleTO[].class, roleId));
    }

    @Override
    public Integer count() {
        //return getRestTemplate().getForObject(baseUrl + "role/count.json", Integer.class);
        throw new UnsupportedOperationException();
    }

    @Override
    public RoleTO create(final RoleTO roleTO) {
        return getRestTemplate().postForObject(baseUrl + "role/create", roleTO, RoleTO.class);
    }

    @Override
    public RoleTO delete(final Long roleId) {
        return getRestTemplate().getForObject(baseUrl + "role/delete/{roleId}", RoleTO.class, roleId);
    }

    @Override
    public List<RoleTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "role/list.json", RoleTO[].class));
    }

    @Override
    public List<RoleTO> list(final int page, final int size) {
        //return Arrays.asList(getRestTemplate().getForObject(baseURL + "role/list.json", RoleTO[].class, page, size));
        throw new UnsupportedOperationException();
    }

    @Override
    public RoleTO parent(final Long roleId) {
        return getRestTemplate().getForObject(baseUrl + "role/parent/{roleId}.json", RoleTO.class, roleId);
    }

    @Override
    public RoleTO read(final Long roleId) {
        return getRestTemplate().getForObject(baseUrl + "role/read/{roleId}.json", RoleTO.class, roleId);
    }

    @Override
    public List<RoleTO> search(final NodeCond searchCondition) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "role/search", searchCondition, RoleTO[].class));
    }

    @Override
    public List<RoleTO> search(final NodeCond searchCondition, final int page, final int size) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "role/search/{page}/{size}", searchCondition,
                RoleTO[].class, page, size));
    }

    @Override
    public int searchCount(final NodeCond searchCondition) {
        return getRestTemplate().postForObject(baseUrl + "role/search/count.json", searchCondition, Integer.class);
    }

    @Override
    public RoleTO selfRead(final Long roleId) {
        return getRestTemplate().getForObject(baseUrl + "role/selfRead/{roleId}", RoleTO.class, roleId);
    }

    @Override
    public RoleTO update(final Long roleId, final RoleMod roleMod) {
        return getRestTemplate().postForObject(baseUrl + "role/update", roleMod, RoleTO.class);
    }
}
