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
package org.apache.syncope.client.cli.commands.entitlement;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.commands.role.RoleSyncopeOperations;
import org.apache.syncope.client.cli.commands.user.UserSyncopeOperations;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;

public class EntitlementSyncopeOperations {

    private final PlatformInfo platformInfo = SyncopeServices.get(SyncopeService.class).platform();

    private final UserSyncopeOperations userSyncopeOperations = new UserSyncopeOperations();

    private final RoleSyncopeOperations roleSyncopeOperations = new RoleSyncopeOperations();

    public Set<String> list() {
        return platformInfo.getEntitlements();
    }

    public boolean exists(final String entitlement) {
        return list().contains(entitlement);
    }

    public Set<String> usernameEntitlements(final String username) {
        final Set<String> entitlements = new TreeSet<>();
        final UserTO userTO = userSyncopeOperations.read(username);
        for (final String role : userTO.getRoles()) {
            entitlements.addAll(roleSyncopeOperations.read(role).getEntitlements());
        }
        return entitlements;
    }

    public Set<String> userIdEntitlements(final String userId) {
        final Set<String> entitlements = new TreeSet<>();
        final UserTO userTO = userSyncopeOperations.read(userId);
        for (final String role : userTO.getRoles()) {
            entitlements.addAll(roleSyncopeOperations.read(role).getEntitlements());
        }
        return entitlements;
    }

    public Set<String> entitlementsPerRole(final String roleId) {
        return roleSyncopeOperations.read(roleId).getEntitlements();
    }

    public Set<RoleTO> rolePerEntitlements(final String entitlement) {
        final Set<RoleTO> roles = new HashSet<>();
        for (final RoleTO role : roleSyncopeOperations.list()) {
            if (role.getEntitlements().contains(entitlement)) {
                roles.add(role);
            }
        }
        return roles;
    }
}
