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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RoleITCase extends AbstractITCase {

    public static RoleTO getSampleRoleTO(final String name) {
        RoleTO role = new RoleTO();
        role.setKey(name + getUUIDString());
        role.getRealms().add("/even");
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);

        return role;
    }

    @Test
    public void list() {
        List<RoleTO> roleTOs = ROLE_SERVICE.list();
        assertNotNull(roleTOs);
        assertFalse(roleTOs.isEmpty());
        roleTOs.forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        RoleTO roleTO = ROLE_SERVICE.read("Search for realm evenTwo");
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().contains(IdRepoEntitlement.USER_READ));
    }

    @Test
    public void create() {
        RoleTO role = new RoleTO();
        role.getRealms().add(SyncopeConstants.ROOT_REALM);
        role.getRealms().add("/even/two");
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);

        try {
            createRole(role);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRole, e.getType());
        }

        role.setKey("new" + getUUIDString());
        role = createRole(role);
        assertNotNull(role);
    }

    @Test
    public void createWithTilde() {
        RoleTO role = new RoleTO();
        role.getRealms().add(SyncopeConstants.ROOT_REALM);
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);
        role.setKey("new~" + getUUIDString());
        role = createRole(role);
        assertNotNull(role);
    }

    @Test
    public void update() {
        RoleTO role = getSampleRoleTO("update");
        role = createRole(role);
        assertNotNull(role);

        assertFalse(role.getEntitlements().contains(FlowableEntitlement.WORKFLOW_TASK_LIST));
        assertFalse(role.getRealms().contains("/even/two"));

        role.getEntitlements().add(FlowableEntitlement.WORKFLOW_TASK_LIST);
        role.getRealms().add("/even/two");

        ROLE_SERVICE.update(role);

        role = ROLE_SERVICE.read(role.getKey());
        assertTrue(role.getEntitlements().contains(FlowableEntitlement.WORKFLOW_TASK_LIST));
        assertTrue(role.getRealms().contains("/even/two"));
    }

    @Test
    public void delete() {
        RoleTO role = getSampleRoleTO("delete");
        Response response = ROLE_SERVICE.create(role);

        RoleTO actual = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(actual);

        ROLE_SERVICE.delete(actual.getKey());

        try {
            ROLE_SERVICE.read(actual.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void dynMembership() {
        UserTO bellini = USER_SERVICE.read("bellini");
        assertTrue(bellini.getDynRoles().isEmpty());

        RoleTO role = null;
        try {
            role = getSampleRoleTO("dynMembership");
            role.setDynMembershipCond("cool==true");
            Response response = ROLE_SERVICE.create(role);
            role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
            assertNotNull(role);

            bellini = USER_SERVICE.read("bellini");
            assertTrue(bellini.getDynRoles().contains(role.getKey()));

            role.setDynMembershipCond("cool==false");
            ROLE_SERVICE.update(role);

            bellini = USER_SERVICE.read("bellini");
            assertTrue(bellini.getDynMemberships().isEmpty());
        } finally {
            if (role != null) {
                ROLE_SERVICE.delete(role.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1472() {
        DynRealmTO dynRealmTO = new DynRealmTO();
        dynRealmTO.setKey("dynRealm");
        dynRealmTO.getDynMembershipConds().put(AnyTypeKind.USER.name(), "username=~rossini");
        DYN_REALM_SERVICE.create(dynRealmTO);

        // 1. associate role Other again to /odd realm and twice to dynRealm
        RoleTO roleTO = ROLE_SERVICE.read("Other");
        roleTO.getRealms().add("/odd");
        roleTO.getDynRealms().add("dynRealm");
        roleTO.getDynRealms().add("dynRealm");
        ROLE_SERVICE.update(roleTO);

        // 2. update by removing realm and dynamic realm
        roleTO = ROLE_SERVICE.read("Other");
        roleTO.getRealms().remove("/odd");
        roleTO.getDynRealms().remove("dynRealm");
        ROLE_SERVICE.update(roleTO);

        roleTO = ROLE_SERVICE.read("Other");

        assertFalse(roleTO.getRealms().contains("/odd"), "Should not contain removed realms");
        assertFalse(roleTO.getDynRealms().contains("dynRealm"), "Should not contain removed dynamic realms");
    }
}
