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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class RoleITCase extends AbstractITCase {

    public static RoleTO getSampleRoleTO(final String name) {
        RoleTO role = new RoleTO();
        role.setKey(name + getUUIDString());
        role.getRealms().add("/even");
        role.getEntitlements().add(StandardEntitlement.LOG_SET_LEVEL);

        return role;
    }

    @Test
    public void list() {
        List<RoleTO> roleTOs = roleService.list();
        assertNotNull(roleTOs);
        assertFalse(roleTOs.isEmpty());
        for (RoleTO instance : roleTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void read() {
        RoleTO roleTO = roleService.read("Search for realm evenTwo");
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().contains(StandardEntitlement.USER_READ));
    }

    @Test
    public void create() {
        RoleTO role = new RoleTO();
        role.getRealms().add(SyncopeConstants.ROOT_REALM);
        role.getRealms().add("/even/two");
        role.getEntitlements().add(StandardEntitlement.LOG_LIST);
        role.getEntitlements().add(StandardEntitlement.LOG_SET_LEVEL);

        try {
            createRole(role);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRole, e.getType());
        }

        role.setKey("new" + getUUIDString());
        role = createRole(role);
        assertNotNull(role);
    }

    @Test
    public void update() {
        RoleTO role = getSampleRoleTO("update");
        role = createRole(role);
        assertNotNull(role);

        assertFalse(role.getEntitlements().contains(StandardEntitlement.WORKFLOW_TASK_LIST));
        assertFalse(role.getRealms().contains("/even/two"));

        role.getEntitlements().add(StandardEntitlement.WORKFLOW_TASK_LIST);
        role.getRealms().add("/even/two");

        roleService.update(role);

        role = roleService.read(role.getKey());
        assertTrue(role.getEntitlements().contains(StandardEntitlement.WORKFLOW_TASK_LIST));
        assertTrue(role.getRealms().contains("/even/two"));
    }

    @Test
    public void delete() {
        RoleTO role = getSampleRoleTO("delete");
        Response response = roleService.create(role);

        RoleTO actual = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(actual);

        roleService.delete(actual.getKey());

        try {
            roleService.read(actual.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void dynMembership() {
        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynRoles().isEmpty());

        RoleTO role = getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = roleService.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        assertTrue(userService.read(
                "c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynRoles().contains(role.getKey()));

        role.setDynMembershipCond("cool==false");
        roleService.update(role);

        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynMemberships().isEmpty());
    }
}
