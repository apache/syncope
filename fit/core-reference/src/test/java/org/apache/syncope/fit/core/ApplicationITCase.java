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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.ApplicationService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class ApplicationITCase extends AbstractITCase {

    @Test
    public void read() {
        ApplicationTO mightyApp = applicationService.read("mightyApp");
        assertNotNull(mightyApp);
        assertEquals(2, mightyApp.getPrivileges().size());
        assertTrue(mightyApp.getPrivileges().stream().anyMatch(privilege -> "postMighty".equals(privilege.getKey())));

        PrivilegeTO getMighty = applicationService.readPrivilege("getMighty");
        assertNotNull(getMighty);
        assertEquals("mightyApp", getMighty.getApplication());

        RoleTO role = roleService.read("Other");
        assertFalse(role.getPrivileges().isEmpty());
        assertEquals(1, role.getPrivileges().size());
        assertTrue(role.getPrivileges().stream().anyMatch("postMighty"::equals));
    }

    @Test
    public void crud() {
        // 1. create application
        ApplicationTO application = new ApplicationTO();
        application.setKey(UUID.randomUUID().toString());

        PrivilegeTO privilegeTO = new PrivilegeTO();
        privilegeTO.setKey(UUID.randomUUID().toString());
        privilegeTO.setSpec("{ \"one\": true }");
        application.getPrivileges().add(privilegeTO);

        privilegeTO = new PrivilegeTO();
        privilegeTO.setKey(UUID.randomUUID().toString());
        privilegeTO.setSpec("{ \"two\": true }");
        application.getPrivileges().add(privilegeTO);

        privilegeTO = new PrivilegeTO();
        privilegeTO.setKey(UUID.randomUUID().toString());
        privilegeTO.setSpec("{ \"three\": true }");
        application.getPrivileges().add(privilegeTO);

        Response response = applicationService.create(application);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        application = getObject(response.getLocation(), ApplicationService.class, ApplicationTO.class);
        assertNotNull(application);
        assertNull(application.getDescription());
        assertEquals(3, application.getPrivileges().size());

        // 2. update application
        application.setDescription("A description");
        application.getPrivileges().remove(1);

        applicationService.update(application);

        application = applicationService.read(application.getKey());
        assertNotNull(application);
        assertNotNull(application.getDescription());
        assertEquals(2, application.getPrivileges().size());

        // 3. assign application's privileges to a new role
        RoleTO role = new RoleTO();
        role.setKey("privileged");
        role.getPrivileges().addAll(
                application.getPrivileges().stream().map(EntityTO::getKey).collect(Collectors.toList()));

        response = roleService.create(role);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);
        assertEquals(2, role.getPrivileges().size());

        // 4. delete application => delete privileges
        applicationService.delete(application.getKey());

        try {
            applicationService.read(application.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        role = roleService.read(role.getKey());
        assertNotNull(role);
        assertTrue(role.getPrivileges().isEmpty());
    }
}
