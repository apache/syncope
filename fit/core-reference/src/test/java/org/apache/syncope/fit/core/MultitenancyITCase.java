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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.List;
import java.util.Locale;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultitenancyITCase extends AbstractITCase {

    @BeforeAll
    public static void restSetup() {
        if (!domainService.list().isEmpty()) {
            clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");

            String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
            if (StringUtils.isNotBlank(envContentType)) {
                clientFactory.setContentType(envContentType);
            }
            LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

            adminClient = clientFactory.create(ADMIN_UNAME, "password2");
        }
    }

    @BeforeEach
    public void multitenancyCheck() {
        assumeFalse(domainService.list().isEmpty());
    }

    @Test
    public void masterOnly() {
        try {
            adminClient.getService(DomainService.class).read("Two");
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        try {
            adminClient.getService(LoggerService.class).list(LoggerType.LOG);
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        adminClient.getService(LoggerService.class).list(LoggerType.AUDIT);
    }

    @Test
    public void readPlainSchemas() {
        assertEquals(12, adminClient.getService(SchemaService.class).
                search(new SchemaQuery.Builder().type(SchemaType.PLAIN).build()).size());
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

        Response response = adminClient.getService(UserService.class).create(user, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
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

            ItemTO item = new ItemTO();
            item.setIntAttrName("username");
            item.setExtAttrName("cn");
            item.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(item);

            item = new ItemTO();
            item.setPassword(true);
            item.setIntAttrName("password");
            item.setExtAttrName("userPassword");
            item.setPurpose(MappingPurpose.BOTH);
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new ItemTO();
            item.setIntAttrName("key");
            item.setPurpose(MappingPurpose.BOTH);
            item.setExtAttrName("sn");
            item.setMandatoryCondition("true");
            mapping.add(item);

            item = new ItemTO();
            item.setIntAttrName("email");
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

            response = adminClient.getService(TaskService.class).create(TaskType.PULL, task);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            task = adminClient.getService(TaskService.class).read(TaskType.PULL,
                    StringUtils.substringAfterLast(response.getLocation().toASCIIString(), "/"), true);
            assertNotNull(resource);

            // pull
            ExecTO execution = AbstractTaskITCase.execProvisioningTask(
                    adminClient.getService(TaskService.class), TaskType.PULL, task.getKey(), MAX_WAIT_SECONDS, false);

            // verify execution status
            String status = execution.getStatus();
            assertNotNull(status);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(status));

            // verify that pulled user is found
            if (ElasticsearchDetector.isElasticSearchEnabled(syncopeService)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            PagedResult<UserTO> matchingUsers = adminClient.getService(UserService.class).search(new AnyQuery.Builder().
                    realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").query()).
                    build());
            assertNotNull(matchingUsers);
            assertEquals(1, matchingUsers.getResult().size());

            // SYNCOPE-1374
            String pullFromLDAPKey = matchingUsers.getResult().get(0).getKey();

            assertEquals(0, adminClient.getService(TaskService.class).
                    search(new TaskQuery.Builder(TaskType.PROPAGATION).
                            anyTypeKind(AnyTypeKind.USER).entityKey(pullFromLDAPKey).build()).getSize());

            PushTaskTO pushTask = new PushTaskTO();
            pushTask.setPerformUpdate(true);
            pushTask.setMatchingRule(MatchingRule.UPDATE);
            adminClient.getService(ReconciliationService.class).
                    push(new ReconQuery.Builder(AnyTypeKind.USER.name(), resource.getKey()).
                            anyKey(pullFromLDAPKey).build(), pushTask);

            assertEquals(1, adminClient.getService(TaskService.class).
                    search(new TaskQuery.Builder(TaskType.PROPAGATION).
                            anyTypeKind(AnyTypeKind.USER).entityKey(pullFromLDAPKey).build()).getSize());
        } finally {
            adminClient.getService(ResourceService.class).delete(resource.getKey());
        }
    }

    @Test
    public void issueSYNCOPE1377() {
        try {
            new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("NotExisting").create().
                    getService(UserSelfService.class).
                    create(UserITCase.getUniqueSampleTO("syncope1377@syncope.apache.org"), true);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getMessage().contains("NotExisting"));
        }
    }
}
