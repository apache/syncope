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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private RoleDataBinder binder;

    @Autowired
    private RoleDAO roleDAO;

    @Test
    void get() {
        RoleTO role = binder.getRoleTO(roleDAO.findById("User reviewer").orElseThrow());
        assertEquals("User reviewer", role.getKey());
        assertTrue(role.getEntitlements().containsAll(
                Set.of(IdRepoEntitlement.USER_READ, IdRepoEntitlement.USER_SEARCH)));
        assertEquals(Set.of("/odd", "/even"), Set.copyOf(role.getRealms()));
    }

    @Test
    void createUpdate() {
        RoleTO roleTO = new RoleTO();
        roleTO.setKey("testRole");
        roleTO.getEntitlements().addAll(List.of(IdRepoEntitlement.USER_READ, IdRepoEntitlement.GROUP_READ));
        roleTO.getRealms().addAll(List.of("/even/two", "/even/three"));

        Role role = roleDAO.save(binder.create(roleTO));

        RoleTO created = binder.getRoleTO(role);
        assertEquals("testRole", created.getKey());
        assertEquals(Set.of(IdRepoEntitlement.USER_READ, IdRepoEntitlement.GROUP_READ),
                created.getEntitlements());
        assertEquals(List.of("/even/two"), created.getRealms());

        created.getEntitlements().clear();
        created.getEntitlements().add(IdRepoEntitlement.USER_UPDATE);
        created.getRealms().clear();
        created.getRealms().add("/odd");
        binder.update(role, created);

        RoleTO updated = binder.getRoleTO(role);
        assertEquals(Set.of(IdRepoEntitlement.USER_UPDATE), updated.getEntitlements());
        assertEquals(List.of("/odd"), updated.getRealms());
    }
}
