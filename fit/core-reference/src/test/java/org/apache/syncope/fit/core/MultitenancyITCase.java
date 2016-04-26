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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class MultitenancyITCase extends AbstractITCase {

    @BeforeClass
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");

        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            clientFactory.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

        adminClient = clientFactory.create(ADMIN_UNAME, "password2");
    }

    @Test
    public void masterOnly() {
        try {
            adminClient.getService(DomainService.class).read("Two");
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        try {
            adminClient.getService(LoggerService.class).list(LoggerType.LOG);
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        adminClient.getService(LoggerService.class).list(LoggerType.AUDIT);
    }

    @Test
    public void readPlainSchemas() {
        assertEquals(11, adminClient.getService(SchemaService.class).
                list(new SchemaQuery.Builder().type(SchemaType.PLAIN).build()).size());
    }

    @Test
    public void readRealm() {
        List<RealmTO> realms = adminClient.getService(RealmService.class).list();
        assertEquals(1, realms.size());
        assertEquals(SyncopeConstants.ROOT_REALM, realms.get(0).getName());
    }

    @Test
    public void createUser() {
        assertNull(adminClient.getService(RealmService.class).list().get(0).getPasswordPolicy());

        UserTO user = new UserTO();
        user.setRealm(SyncopeConstants.ROOT_REALM);
        user.setUsername(getUUIDString());
        user.setPassword("password");

        Response response = adminClient.getService(UserService.class).create(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getAny();
        assertNotNull(user);
    }

    @Test
    public void createResourceAndPull() {
        // read connector
        ConnInstanceTO conn = adminClient.getService(ConnectorService.class).
                read("b7ea96c3-c633-488b-98a0-b52ac35850f7", Locale.ENGLISH.getLanguage());
        assertNotNull(conn);
        assertEquals("LDAP", conn.getDisplayName());

        // prepare resource
        ResourceTO resource = new ResourceTO();
        resource.setKey("new-ldap-resource");
        resource.setConnector(conn.getKey());

        try {
            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provisionTO);

            MappingTO mapping = new MappingTO();
            mapping.setConnObjectLink("'uid=' + username + ',ou=people,o=isp'");
            provisionTO.setMapping(mapping);

            MappingItemTO item = new MappingItemTO();
            item.setIntAttrName("username");
            item.setIntMappingType(IntMappingType.Username);
            item.setExtAttrName("cn");
            item.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(item);

            item = new MappingItemTO();
            item.setPassword(true);
            item.setIntMappingType(IntMappingType.Password);
            item.setExtAttrName("userPassword");
            item.setPurpose(MappingPurpose.BOTH);
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.UserKey);
            item.setPurpose(MappingPurpose.BOTH);
            item.setExtAttrName("sn");
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new MappingItemTO();
            item.setIntAttrName("email");
            item.setIntMappingType(IntMappingType.UserPlainSchema);
            item.setPurpose(MappingPurpose.BOTH);
            item.setExtAttrName("mail");
            mapping.add(item);

            // create resource
            Response response = adminClient.getService(ResourceService.class).create(resource);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            resource = adminClient.getService(ResourceService.class).read(resource.getKey());
            assertNotNull(resource);

            // create pull task
            PullTaskTO task = new PullTaskTO();
            task.setName("LDAP Pull Task");
            task.setActive(true);
            task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            task.setResource(resource.getKey());
            task.setPullMode(PullMode.FULL_RECONCILIATION);
            task.setPerformCreate(true);

            response = adminClient.getService(TaskService.class).create(task);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            task = adminClient.getService(TaskService.class).read(
                    StringUtils.substringAfterLast(response.getLocation().toASCIIString(), "/"), true);
            assertNotNull(resource);

            // pull
            ExecTO execution = AbstractTaskITCase.execProvisioningTask(
                    adminClient.getService(TaskService.class), task.getKey(), 50, false);

            // verify execution status
            String status = execution.getStatus();
            assertNotNull(status);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(status));

            // verify that pulled user is found
            PagedResult<UserTO> matchingUsers = adminClient.getService(UserService.class).search(
                    new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").query()).
                    build());
            assertNotNull(matchingUsers);
            assertEquals(1, matchingUsers.getResult().size());
        } finally {
            adminClient.getService(ResourceService.class).delete(resource.getKey());
        }
    }
}
