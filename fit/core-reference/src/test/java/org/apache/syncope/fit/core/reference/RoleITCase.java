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
package org.apache.syncope.fit.core.reference;

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
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RoleITCase extends AbstractITCase {

    public static RoleTO getSampleRoleTO(final String name) {
        RoleTO role = new RoleTO();
        role.setName(name + getUUIDString());
        role.getRealms().add("/even");
        role.getEntitlements().add(Entitlement.LOG_SET_LEVEL);

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
        RoleTO roleTO = roleService.read(3L);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().contains(Entitlement.GROUP_READ));
    }

    @Test
    public void create() {
        RoleTO role = new RoleTO();
        role.setName("new" + getUUIDString());
        role.getRealms().add(SyncopeConstants.ROOT_REALM);
        role.getRealms().add("/even/two");
        role.getEntitlements().add(Entitlement.LOG_LIST);
        role.getEntitlements().add(Entitlement.LOG_SET_LEVEL);

        Response response = roleService.create(role);

        RoleTO actual = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(actual);
    }

    @Test
    public void update() {
        RoleTO role = getSampleRoleTO("update");
        Response response = roleService.create(role);

        RoleTO actual = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(actual);

        role = actual;
        assertFalse(role.getEntitlements().contains(Entitlement.WORKFLOW_TASK_LIST));
        assertFalse(role.getRealms().contains("/even/two"));

        role.getEntitlements().add(Entitlement.WORKFLOW_TASK_LIST);
        role.getRealms().add("/even/two");

        roleService.update(role.getKey(), role);

        actual = roleService.read(role.getKey());
        assertTrue(actual.getEntitlements().contains(Entitlement.WORKFLOW_TASK_LIST));
        assertTrue(actual.getRealms().contains("/even/two"));
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
        assertTrue(userService.read(4L).getDynRoles().isEmpty());

        RoleTO role = getSampleRoleTO("dynMembership");
        role.setDynMembershipCond("cool==true");
        Response response = roleService.create(role);
        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        assertTrue(userService.read(4L).getDynRoles().contains(role.getKey()));

        role.setDynMembershipCond("cool==false");
        roleService.update(role.getKey(), role);

        assertTrue(userService.read(4L).getDynGroups().isEmpty());
    }
}
