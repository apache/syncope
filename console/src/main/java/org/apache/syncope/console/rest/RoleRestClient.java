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

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.ConnObjectTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.SyncopeSession;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractAttributableRestClient {

    @Override
    public Integer count() {
        return SyncopeSession.get().getRestTemplate().getForObject(baseURL + "role/count.json", Integer.class);
    }

    public List<RoleTO> list() {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "role/list.json", RoleTO[].class));
    }

    @Override
    public List<RoleTO> list(final int page, final int size) {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "role/list.json", RoleTO[].class, page, size));
    }

    @Override
    public Integer searchCount(final NodeCond searchCond) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/search/count.json", searchCond, Integer.class);
    }

    @Override
    public List<RoleTO> search(final NodeCond searchCond, final int page, final int size)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/search/{page}/{size}", searchCond, RoleTO[].class, page, size));
    }

    @Override
    public ConnObjectTO getRemoteObject(final String resourceName, final String objectId)
            throws SyncopeClientCompositeErrorException {

        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "/resource/{resourceName}/read/ROLE/{objectId}.json",
                ConnObjectTO.class, resourceName, objectId);
    }

    public RoleTO create(final RoleTO roleTO) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/create", roleTO, RoleTO.class);
    }

    public RoleTO read(final Long id) {
        RoleTO roleTO = null;

        try {
            roleTO = SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "role/read/{roleId}.json", RoleTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a role", e);
        }
        return roleTO;
    }

    public RoleTO update(final RoleMod roleMod) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/update", roleMod, RoleTO.class);
    }

    @Override
    public RoleTO delete(final Long id) {
        return SyncopeSession.get().getRestTemplate().getForObject(baseURL + "role/delete/{roleId}", RoleTO.class, id);
    }
}
