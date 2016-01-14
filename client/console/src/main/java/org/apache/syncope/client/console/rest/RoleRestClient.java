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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Group's services.
 */
@Component
public class RoleRestClient extends BaseRestClient {

    private static final long serialVersionUID = 1L;

    public void delete(final String key) {
        getService(RoleService.class).delete(key);
    }

    public RoleTO read(final String key) {
        return getService(RoleService.class).read(key);
    }

    public void update(final RoleTO roleTO) {
        getService(RoleService.class).update(roleTO);
    }

    public void create(final RoleTO roleTO) {
        getService(RoleService.class).create(roleTO);
    }

    public List<RoleTO> getAll() {
        return getService(RoleService.class).list();
    }

    public List<RoleTO> list() {
        return getService(RoleService.class).list();
    }

    public int count() {
        return getService(RoleService.class).list().size();
    }

    public List<String> getAllAvailableEntitlements() {
        return new ArrayList<>(getSyncopeService().info().getEntitlements());
    }
}
