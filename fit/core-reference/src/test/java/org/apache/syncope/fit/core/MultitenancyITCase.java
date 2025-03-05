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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultitenancyITCase extends AbstractITCase {

    @BeforeEach
    public void multitenancyCheck() {
        assumeTrue(domainOps.list().stream().anyMatch(d -> "Two".equals(d.getKey())));

        List<Domain> initial = domainOps.list();
        assertNotNull(initial);
        assumeTrue(initial.stream().anyMatch(domain -> "Two".equals(domain.getKey())));

        CLIENT_FACTORY = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");

        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            CLIENT_FACTORY.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", CLIENT_FACTORY.getContentType().getMediaType());

        ADMIN_CLIENT = CLIENT_FACTORY.create(ADMIN_UNAME, "password2");
    }

    @Test
    public void readPlainSchemas() {
        assertEquals(1, ADMIN_CLIENT.getService(SchemaService.class).
                search(new SchemaQuery.Builder().type(SchemaType.PLAIN).build()).size());
    }

    @Test
    public void readRealm() {
        PagedResult<RealmTO> realms = ADMIN_CLIENT.getService(RealmService.class).
                search(new RealmQuery.Builder().keyword("*").build());
        assertEquals(1, realms.getTotalCount());
        assertEquals(1, realms.getResult().size());
        assertEquals(SyncopeConstants.ROOT_REALM, realms.getResult().getFirst().getName());
    }

    @Test
    public void createUser() {
        assertNull(ADMIN_CLIENT.getService(RealmService.class).
                search(new RealmQuery.Builder().keyword("*").build()).getResult().getFirst().getPasswordPolicy());

        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        userCR.setUsername(getUUIDString());
        userCR.setPassword("password");

        Response response = ADMIN_CLIENT.getService(UserService.class).create(userCR);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        UserTO user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(user);
    }

    @Test
    public void createResourceAndPull() {
        // read connector
        ConnInstanceTO conn = ADMIN_CLIENT.getService(ConnectorService.class).
                read("b7ea96c3-c633-488b-98a0-b52ac35850f7", Locale.ENGLISH.getLanguage());
        assertNotNull(conn);
        assertEquals("LDAP", conn.getDisplayName());

        // prepare resource
        ResourceTO resource = new ResourceTO();
        resource.setKey("new-ldap-resource");
        resource.setConnector(conn.getKey());

        try {
            Provision provisionTO = new Provision();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resource.getProvisions().add(provisionTO);

            Mapping mapping = new Mapping();
            mapping.setConnObjectLink("'uid=' + username + ',ou=people,o=isp'");
            provisionTO.setMapping(mapping);

            Item item = new Item();
            item.setIntAttrName("username");
            item.setExtAttrName("cn");
            item.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(item);

            item = new Item();
            item.setPassword(true);
            item.setIntAttrName("password");
            item.setExtAttrName("userPassword");
            item.setPurpose(MappingPurpose.BOTH);
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new Item();
            item.setIntAttrName("key");
            item.setPurpose(MappingPurpose.BOTH);
            item.setExtAttrName("sn");
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new Item();
            item.setIntAttrName("email");
            item.setPurpose(MappingPurpose.BOTH);
            item.setExtAttrName("mail");
            mapping.add(item);

            // create resource
            Response response = ADMIN_CLIENT.getService(ResourceService.class).create(resource);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            resource = ADMIN_CLIENT.getService(ResourceService.class).read(resource.getKey());
            assertNotNull(resource);

            // create pull task
            PullTaskTO task = new PullTaskTO();
            task.setName("LDAP Pull Task");
            task.setActive(true);
            task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            task.setResource(resource.getKey());
            task.setPullMode(PullMode.FULL_RECONCILIATION);
            task.setPerformCreate(true);

            response = ADMIN_CLIENT.getService(TaskService.class).create(TaskType.PULL, task);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            task = ADMIN_CLIENT.getService(TaskService.class).read(TaskType.PULL,
                    StringUtils.substringAfterLast(response.getLocation().toASCIIString(), "/"), true);
            assertNotNull(resource);

            // pull
            ExecTO execution = AbstractTaskITCase.execSchedTask(
                    ADMIN_CLIENT.getService(TaskService.class), TaskType.PULL, task.getKey(), MAX_WAIT_SECONDS, false);

            // verify execution status
            String status = execution.getStatus();
            assertNotNull(status);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(status));

            // verify that pulled user is found
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            PagedResult<UserTO> matchingUsers = ADMIN_CLIENT.getService(UserService.class).
                    search(new AnyQuery.Builder().
                            realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getUserSearchConditionBuilder().
                                    is("username").equalTo("pullFromLDAP").query()).
                            build());
            assertNotNull(matchingUsers);
            assertEquals(1, matchingUsers.getResult().size());

            // SYNCOPE-1374
            String pullFromLDAPKey = matchingUsers.getResult().getFirst().getKey();

            assertEquals(0, ADMIN_CLIENT.getService(TaskService.class).
                    search(new TaskQuery.Builder(TaskType.PROPAGATION).
                            anyTypeKind(AnyTypeKind.USER).entityKey(pullFromLDAPKey).build()).getSize());

            PushTaskTO pushTask = new PushTaskTO();
            pushTask.setPerformUpdate(true);
            pushTask.setMatchingRule(MatchingRule.UPDATE);
            ADMIN_CLIENT.getService(ReconciliationService.class).
                    push(new ReconQuery.Builder(AnyTypeKind.USER.name(), resource.getKey()).
                            anyKey(pullFromLDAPKey).build(), pushTask);

            assertEquals(1, ADMIN_CLIENT.getService(TaskService.class).
                    search(new TaskQuery.Builder(TaskType.PROPAGATION).
                            anyTypeKind(AnyTypeKind.USER).entityKey(pullFromLDAPKey).build()).getSize());
        } finally {
            ADMIN_CLIENT.getService(ResourceService.class).delete(resource.getKey());
        }
    }

    @Test
    public void issueSYNCOPE1377() {
        try {
            new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("NotExisting").
                    create(ADMIN_UNAME, ADMIN_PWD).
                    getService(UserSelfService.class).
                    create(UserITCase.getUniqueSample("syncope1377@syncope.apache.org"));
            fail("This should not happen");
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().contains("Could not find domain NotExisting"));
        }
    }
}
