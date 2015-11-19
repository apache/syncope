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
package org.apache.syncope.client.cli.commands.role;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.rest.api.service.RoleService;

public class RoleSyncopeOperations {

    private final RoleService roleService = SyncopeServices.get(RoleService.class);

    public List<RoleTO> list() {
        return roleService.list();
    }

    public RoleTO read(final String role) {
        return roleService.read(role);
    }

    public void delete(final String role) {
        roleService.delete(role);
    }
}
