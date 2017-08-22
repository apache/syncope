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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.OrgUnitTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceHistoryConfTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOListQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class ResourceITCase extends AbstractITCase {

    private ResourceTO buildResourceTO(final String resourceKey) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new ItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("key");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setConnObjectKey(false);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(item);

        return resourceTO;
    }

    @Test
    public void getPropagationActionsClasses() {
        Set<String> actions = syncopeService.platform().getPropagationActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        String resourceKey = "ws-target-resource-create";
        ResourceTO resourceTO = buildResourceTO(resourceKey);

        Response response = resourceService.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(resourceTO);

        // check for existence
        resourceTO = resourceService.read(resourceKey);
        assertNotNull(resourceTO);
    }

    @Test
    public void createOverridingProps() {
        String resourceKey = "overriding-conn-conf-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new ItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("key");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setConnObjectKey(false);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(item);

        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ConnConfProperty prop = new ConnConfProperty();
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setType("java.lang.String");
        schema.setName("endpoint");
        schema.setRequired(true);
        prop.setSchema(schema);
        prop.getValues().add("http://invalidurl/");

        Set<ConnConfProperty> connectorConfigurationProperties = new HashSet<>(Arrays.asList(prop));
        resourceTO.getConfOverride().addAll(connectorConfigurationProperties);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check the existence
        actual = resourceService.read(resourceKey);
        assertNotNull(actual);
        assertNull(actual.getPropagationPriority());
    }

    @Test
    public void createWithSingleMappingItem() {
        String resourceKey = RESOURCE_NAME_CREATE_SINGLE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setConnObjectKeyItem(item);

        provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.GROUP.name());
        provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
        resourceTO.getProvisions().add(provisionTO);

        mapping = new MappingTO();
        provisionTO.setMapping(mapping);
        item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("groupId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.PULL);
        mapping.setConnObjectKeyItem(item);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).get().getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getItems());
        assertNotNull(actual.getProvision(AnyTypeKind.GROUP.name()).get().getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems());
        assertEquals(MappingPurpose.PULL,
                actual.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getConnObjectKeyItem().getPurpose());
        assertEquals(MappingPurpose.PROPAGATION,
                actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getConnObjectKeyItem().getPurpose());
    }

    @Test
    public void createWithInvalidMapping() {
        String resourceKey = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        mapping.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setExtAttrName("email");
        // missing intAttrName ...
        mapping.add(item);

        try {
            createResource(resourceTO);
            fail("Create should not have worked");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            assertEquals("intAttrName", e.getElements().iterator().next());
        }
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithoutExtAttr() {
        String resourceKey = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        mapping.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setIntAttrName("usernane");
        // missing extAttrName ...
        mapping.add(item);

        createResource(resourceTO);
    }

    @Test
    public void createWithPasswordPolicy() {
        String resourceKey = "res-with-password-policy";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");
        resourceTO.setPasswordPolicy("986d1236-3ac5-4a19-810c-5ab21d79cba1");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check the existence
        actual = resourceService.read(resourceKey);
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals("986d1236-3ac5-4a19-810c-5ab21d79cba1", actual.getPasswordPolicy());
    }

    @Test
    public void updateWithException() {
        try {
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setKey("resourcenotfound");
            resourceService.update(resourceTO);

            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void update() {
        String resourceKey = RESOURCE_NAME_UPDATE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5aa5b8be-7521-481a-9651-c557aea078c1");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        // Update with an existing and already assigned mapping
        ItemTO item = new ItemTO();
        item.setKey("cc973ed6-d031-4790-adab-fc059ac0c818");
        item.setExtAttrName("test3");
        item.setIntAttrName("fullname");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            item = new ItemTO();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("fullname");
            item.setPurpose(MappingPurpose.BOTH);
            mapping.add(item);
        }
        item = new ItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("key");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        resourceService.update(resourceTO);
        ResourceTO actual = resourceService.read(resourceTO.getKey());
        assertNotNull(actual);

        // check for existence
        Collection<ItemTO> mapItems = actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getItems();
        assertNotNull(mapItems);
        assertEquals(4, mapItems.size());
    }

    @Test
    public void deleteWithException() {
        try {
            resourceService.delete("resourcenotfound");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void syncToken() {
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
        resource.setKey(resource.getKey() + getUUIDString());

        AnyObjectTO anyObject = AnyObjectITCase.getSampleTO("syncToken");
        anyObject.getResources().clear();
        anyObject.getResources().add(resource.getKey());
        try {
            // create a new resource
            resource = createResource(resource);
            assertNull(resource.getProvision("PRINTER").get().getSyncToken());

            // create some object on the new resource
            anyObject = createAnyObject(anyObject).getEntity();

            // update sync token
            resourceService.setLatestSyncToken(resource.getKey(), "PRINTER");

            resource = resourceService.read(resource.getKey());
            assertNotNull(resource.getProvision("PRINTER").get().getSyncToken());

            // remove sync token
            resourceService.removeSyncToken(resource.getKey(), "PRINTER");

            resource = resourceService.read(resource.getKey());
            assertNull(resource.getProvision("PRINTER").get().getSyncToken());
        } finally {
            if (anyObject.getKey() != null) {
                anyObjectService.delete(anyObject.getKey());
            }
            resourceService.delete(resource.getKey());
        }
    }

    @Test
    public void delete() {
        String resourceKey = "tobedeleted";

        ResourceTO resource = buildResourceTO(resourceKey);
        Response response = resourceService.create(resource);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        resourceService.delete(resourceKey);

        try {
            resourceService.read(resourceKey);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void orgUnit() {
        String resourceKey = "ws-orgunit";
        ResourceTO resourceTO = buildResourceTO(resourceKey);
        assertNull(resourceTO.getOrgUnit());
        assertNull(resourceTO.getPropagationPriority());

        Response response = resourceService.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(resourceTO);
        assertNull(resourceTO.getOrgUnit());

        OrgUnitTO orgUnit = new OrgUnitTO();
        orgUnit.setConnObjectLink("'ou=' + name + ',o=isp'");
        orgUnit.setObjectClass("organizationalUnit");

        ItemTO item = new ItemTO();
        item.setIntAttrName("name");
        item.setExtAttrName("ou");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.BOTH);
        orgUnit.setConnObjectKeyItem(item);

        resourceTO.setOrgUnit(orgUnit);
        resourceService.update(resourceTO);
        assertNull(resourceTO.getPropagationPriority());

        resourceTO = resourceService.read(resourceKey);
        assertNotNull(resourceTO.getOrgUnit());

        resourceTO.setOrgUnit(null);
        resourceTO.setPropagationPriority(11);
        resourceService.update(resourceTO);

        resourceTO = resourceService.read(resourceKey);
        assertNull(resourceTO.getOrgUnit());
        assertEquals(Integer.valueOf(11), resourceTO.getPropagationPriority());
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = resourceService.list();
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        actuals.forEach(resourceTO -> assertNotNull(resourceTO));
    }

    @Test
    public void read() {
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBVIRATTR);
        assertNotNull(resource);

        Optional<ProvisionTO> provision = resource.getProvision(AnyTypeKind.USER.name());
        assertTrue(provision.isPresent());
        assertFalse(provision.get().getMapping().getItems().isEmpty());
        assertFalse(provision.get().getMapping().getLinkingItems().isEmpty());
    }

    @Test
    public void listConnObjects() {
        List<String> groupKeys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupTO group = GroupITCase.getSampleTO("group");
            group.getResources().add(RESOURCE_NAME_LDAP);
            group = createGroup(group).getEntity();
            groupKeys.add(group.getKey());
        }

        int totalRead = 0;
        Set<String> read = new HashSet<>();
        try {
            ConnObjectTOListQuery.Builder builder = new ConnObjectTOListQuery.Builder().size(10);
            PagedConnObjectTOResult list;
            do {
                list = null;

                boolean succeeded = false;
                // needed because ApacheDS seems to randomly fail when searching with cookie
                for (int i = 0; i < 5 && !succeeded; i++) {
                    try {
                        list = resourceService.listConnObjects(
                                RESOURCE_NAME_LDAP,
                                AnyTypeKind.GROUP.name(),
                                builder.build());
                        succeeded = true;
                    } catch (SyncopeClientException e) {
                        assertEquals(ClientExceptionType.ConnectorException, e.getType());
                    }
                }
                assertNotNull(list);

                totalRead += list.getResult().size();
                read.addAll(list.getResult().stream().
                        map(input -> input.getAttr(ConnIdSpecialName.NAME).get().getValues().get(0)).
                        collect(Collectors.toList()));

                if (list.getPagedResultsCookie() != null) {
                    builder.pagedResultsCookie(list.getPagedResultsCookie());
                }
            } while (list.getPagedResultsCookie() != null);

            assertEquals(totalRead, read.size());
            assertTrue(totalRead >= 10);
        } finally {
            groupKeys.forEach(key -> {
                groupService.delete(key);
            });
        }
    }

    @Test
    public void history() {
        List<ResourceHistoryConfTO> history = resourceHistoryService.list(RESOURCE_NAME_LDAP);
        assertNotNull(history);
        int pre = history.size();

        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        TraceLevel originalTraceLevel = SerializationUtils.clone(ldap.getUpdateTraceLevel());
        assertEquals(TraceLevel.ALL, originalTraceLevel);
        ProvisionTO originalProvision = SerializationUtils.clone(ldap.getProvision(AnyTypeKind.USER.name()).get());
        assertEquals(ObjectClass.ACCOUNT_NAME, originalProvision.getObjectClass());
        boolean originalFlag = ldap.isRandomPwdIfNotProvided();
        assertTrue(originalFlag);

        ldap.setUpdateTraceLevel(TraceLevel.FAILURES);
        ldap.getProvision(AnyTypeKind.USER.name()).get().setObjectClass("ANOTHER");
        ldap.setRandomPwdIfNotProvided(false);
        resourceService.update(ldap);

        ldap = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotEquals(originalTraceLevel, ldap.getUpdateTraceLevel());
        assertNotEquals(
                originalProvision.getObjectClass(), ldap.getProvision(AnyTypeKind.USER.name()).get().getObjectClass());
        assertNotEquals(originalFlag, ldap.isRandomPwdIfNotProvided());

        history = resourceHistoryService.list(RESOURCE_NAME_LDAP);
        assertEquals(pre + 1, history.size());

        resourceHistoryService.restore(history.get(0).getKey());

        ldap = resourceService.read(RESOURCE_NAME_LDAP);
        assertEquals(originalTraceLevel, ldap.getUpdateTraceLevel());
        assertEquals(
                originalProvision.getObjectClass(),
                ldap.getProvision(AnyTypeKind.USER.name()).get().getObjectClass());
        assertEquals(originalFlag, ldap.isRandomPwdIfNotProvided());
    }

    @Test
    public void authorizations() {
        SyncopeClient puccini = clientFactory.create("puccini", ADMIN_PWD);
        ResourceService prs = puccini.getService(ResourceService.class);

        // 1. attempt to read a resource for a connector with a different admin realm: fail
        try {
            prs.read(RESOURCE_NAME_WS1);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 2. read and upate a resource for a connector in the realm for which entitlements are owned: succeed
        try {
            ResourceTO scriptedsql = prs.read(RESOURCE_NAME_DBSCRIPTED);
            assertEquals(TraceLevel.ALL, scriptedsql.getCreateTraceLevel());

            scriptedsql.setCreateTraceLevel(TraceLevel.FAILURES);
            prs.update(scriptedsql);

            scriptedsql = prs.read(RESOURCE_NAME_DBSCRIPTED);
            assertEquals(TraceLevel.FAILURES, scriptedsql.getCreateTraceLevel());
        } finally {
            ResourceTO scriptedsql = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
            scriptedsql.setCreateTraceLevel(TraceLevel.ALL);
            resourceService.update(scriptedsql);
        }
    }

    @Test
    public void issueSYNCOPE323() {
        ResourceTO actual = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(actual);

        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setKey(null);
        try {
            createResource(actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE360() {
        final String name = "SYNCOPE360-" + getUUIDString();
        resourceService.create(buildResourceTO(name));

        ResourceTO resource = resourceService.read(name);
        assertNotNull(resource);
        assertNotNull(resource.getProvision(AnyTypeKind.USER.name()).get().getMapping());

        resource.getProvision(AnyTypeKind.USER.name()).get().setMapping(null);
        resourceService.update(resource);

        resource = resourceService.read(name);
        assertNotNull(resource);
        assertNull(resource.getProvision(AnyTypeKind.USER.name()).get().getMapping());
    }

    @Test
    public void issueSYNCOPE368() {
        final String name = "SYNCOPE368-" + getUUIDString();

        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(name);
        resourceTO.setConnector("74141a3b-0762-4720-a4aa-fc3e374ef3ef");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.GROUP.name());
        provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("name");
        item.setExtAttrName("cn");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new ItemTO();
        item.setIntAttrName("userOwner");
        item.setExtAttrName("owner");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        resourceTO = createResource(resourceTO);
        assertNotNull(resourceTO);
        assertEquals(2, resourceTO.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems().size());
    }

    @Test
    public void issueSYNCOPE418() {
        try {
            resourceService.create(
                    buildResourceTO("http://schemas.examples.org/security/authorization/organizationUnit"));
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidExternalResource, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidKey.name()));
        }
    }

    @Test
    public void issueSYNCOPE493() {
        // create resource with attribute mapping set to NONE and check its propagation
        String resourceKey = RESOURCE_NAME_CREATE_NONE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setConnObjectKeyItem(item);

        ItemTO item2 = new ItemTO();
        item2.setConnObjectKey(false);
        item2.setIntAttrName("gender");
        item2.setExtAttrName("gender");
        item2.setPurpose(MappingPurpose.NONE);
        mapping.add(item2);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).get().getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getItems());
        assertEquals(MappingPurpose.PROPAGATION,
                actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getConnObjectKeyItem().getPurpose());
        actual.getProvision(AnyTypeKind.USER.name()).get().getMapping().getItems().stream().
                filter(itemTO -> ("gender".equals(itemTO.getIntAttrName()))).
                forEach(itemTO -> assertEquals(MappingPurpose.NONE, itemTO.getPurpose()));
    }

    public void issueSYNCOPE645() {
        ResourceTO resource = new ResourceTO();
        resource.setKey("ws-target-resource-basic-save-invalid");

        String connector = resourceService.read("ws-target-resource-1").getConnector();
        resource.setConnector(connector);

        ProvisionTO provision = new ProvisionTO();
        provision.setAnyType(AnyTypeKind.USER.name());
        provision.setObjectClass("__ACCOUNT__");
        resource.getProvisions().add(provision);

        MappingTO mapping = new MappingTO();
        provision.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        // save the resource
        try {
            resourceService.create(resource);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMapping, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE888() {
        String resourceKey = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceKey);
        resourceTO.setConnector("5ffbb4ac-a8c3-4b44-b699-11b398a1ba08");

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        ItemTO item = new ItemTO();
        item.setIntAttrName("key");
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        // Add mapping for a not existing internal attribute
        item = new ItemTO();
        item.setIntAttrName("locatio");
        item.setExtAttrName("location");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        try {
            createResource(resourceTO);
            fail("Create should not have worked");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMapping, e.getType());
            assertEquals(1, e.getElements().size());
            assertEquals("'locatio' not existing", e.getElements().iterator().next());
        }
    }
}
