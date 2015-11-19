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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOListQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ResourceITCase extends AbstractITCase {

    private ResourceTO buildResourceTO(final String resourceName) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserKey);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setConnObjectKey(false);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        return resourceTO;
    }

    @Test
    public void getPropagationActionsClasses() {
        Set<String> actions = syncopeService.info().getPropagationActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        String resourceName = RESOURCE_NAME_CREATE;
        ResourceTO resourceTO = buildResourceTO(resourceName);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check for existence
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createOverridingProps() {
        String resourceName = "overriding-conn-conf-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("uid");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserKey);
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setExtAttrName("fullname");
        item.setIntAttrName("cn");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setConnObjectKey(false);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

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
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
    }

    @Test
    public void createWithSingleMappingItem() {
        String resourceName = RESOURCE_NAME_CREATE_SINGLE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserKey);
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
        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.GroupKey);
        item.setExtAttrName("groupId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.SYNCHRONIZATION);
        mapping.setConnObjectKeyItem(item);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).getMapping().getItems());
        assertNotNull(actual.getProvision(AnyTypeKind.GROUP.name()).getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.GROUP.name()).getMapping().getItems());
        assertEquals(MappingPurpose.SYNCHRONIZATION,
                actual.getProvision(AnyTypeKind.GROUP.name()).getMapping().getConnObjectKeyItem().getPurpose());
        assertEquals(MappingPurpose.PROPAGATION,
                actual.getProvision(AnyTypeKind.USER.name()).getMapping().getConnObjectKeyItem().getPurpose());
    }

    @Test
    public void createWithInvalidMapping() {
        String resourceName = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserKey);
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserPlainSchema);
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
        String resourceName = RESOURCE_NAME_CREATE_WRONG;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserKey);
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setIntAttrName("usernane");
        // missing extAttrName ...
        mapping.add(item);

        createResource(resourceTO);
    }

    @Test
    public void createWithPasswordPolicy() {
        String resourceName = "res-with-password-policy";
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);
        resourceTO.setPasswordPolicy(4L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setExtAttrName("userId");
        item.setIntAttrName("userId");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        // check the existence
        actual = resourceService.read(resourceName);
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
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
        String resourceName = RESOURCE_NAME_UPDATE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(101L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        // Update with an existing and already assigned mapping
        MappingItemTO item = new MappingItemTO();
        item.setKey(112L);
        item.setExtAttrName("test3");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        // Update defining new mappings
        for (int i = 4; i < 6; i++) {
            item = new MappingItemTO();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("fullname");
            item.setIntMappingType(IntMappingType.UserPlainSchema);
            item.setPurpose(MappingPurpose.BOTH);
            mapping.add(item);
        }
        item = new MappingItemTO();
        item.setExtAttrName("username");
        item.setIntAttrName("fullname");
        item.setIntMappingType(IntMappingType.UserKey);
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        resourceService.update(resourceTO);
        ResourceTO actual = resourceService.read(resourceTO.getKey());
        assertNotNull(actual);

        // check for existence
        Collection<MappingItemTO> mapItems = actual.getProvision(AnyTypeKind.USER.name()).getMapping().getItems();
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
    public void updateResetSyncToken() {
        // create resource with sync token
        String resourceName = RESOURCE_NAME_RESETSYNCTOKEN + getUUIDString();
        ResourceTO pre = buildResourceTO(resourceName);

        pre.getProvision(AnyTypeKind.USER.name()).setSyncToken("test");
        resourceService.create(pre);

        pre.getProvision(AnyTypeKind.USER.name()).setSyncToken(null);
        resourceService.update(pre);
        ResourceTO actual = resourceService.read(pre.getKey());
        // check that the synctoken has been reset
        assertNull(actual.getProvision(AnyTypeKind.USER.name()).getSyncToken());
    }

    @Test
    public void delete() {
        String resourceName = "tobedeleted";

        ResourceTO resource = buildResourceTO(resourceName);
        Response response = resourceService.create(resource);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(actual);

        resourceService.delete(resourceName);

        try {
            resourceService.read(resourceName);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        List<ResourceTO> actuals = resourceService.list();
        assertNotNull(actuals);
        assertFalse(actuals.isEmpty());
        for (ResourceTO resourceTO : actuals) {
            assertNotNull(resourceTO);
        }
    }

    @Test
    public void read() {
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBVIRATTR);
        assertNotNull(resource);

        ProvisionTO provision = resource.getProvision(AnyTypeKind.USER.name());
        assertNotNull(provision);
        assertFalse(provision.getMapping().getItems().isEmpty());
        assertFalse(provision.getMapping().getLinkingItems().isEmpty());
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
    public void bulkAction() {
        resourceService.create(buildResourceTO("forBulk1"));
        resourceService.create(buildResourceTO("forBulk2"));

        assertNotNull(resourceService.read("forBulk1"));
        assertNotNull(resourceService.read("forBulk2"));

        final BulkAction bulkAction = new BulkAction();
        bulkAction.setType(BulkAction.Type.DELETE);

        bulkAction.getTargets().add(String.valueOf("forBulk1"));
        bulkAction.getTargets().add(String.valueOf("forBulk2"));

        resourceService.bulk(bulkAction);

        try {
            resourceService.read("forBulk1");
            fail();
        } catch (SyncopeClientException e) {
        }

        try {
            resourceService.read("forBulk2");
            fail();
        } catch (SyncopeClientException e) {
        }
    }

    @Test
    public void anonymous() {
        ResourceService unauthenticated = clientFactory.create().getService(ResourceService.class);
        try {
            unauthenticated.list();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        ResourceService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).
                getService(ResourceService.class);
        assertFalse(anonymous.list().isEmpty());
    }

    @Test
    public void listConnObjects() {
        List<Long> groupKeys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupTO group = GroupITCase.getSampleTO("group");
            group.getResources().add(RESOURCE_NAME_LDAP);
            group = createGroup(group).getAny();
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
                CollectionUtils.collect(list.getResult(), new Transformer<ConnObjectTO, String>() {

                    @Override
                    public String transform(final ConnObjectTO input) {
                        return input.getPlainAttrMap().get("__NAME__").getValues().get(0);
                    }
                }, read);

                if (list.getPagedResultsCookie() != null) {
                    builder.pagedResultsCookie(list.getPagedResultsCookie());
                }
            } while (list.getPagedResultsCookie() != null);

            assertEquals(totalRead, read.size());
            assertTrue(totalRead >= 10);
        } finally {
            for (Long key : groupKeys) {
                groupService.delete(key);
            }
        }
    }

    @Test
    public void issueSYNCOPE360() {
        final String name = "SYNCOPE360-" + getUUIDString();
        resourceService.create(buildResourceTO(name));

        ResourceTO resource = resourceService.read(name);
        assertNotNull(resource);
        assertNotNull(resource.getProvision(AnyTypeKind.USER.name()).getMapping());

        resource.getProvision(AnyTypeKind.USER.name()).setMapping(null);
        resourceService.update(resource);

        resource = resourceService.read(name);
        assertNotNull(resource);
        assertNull(resource.getProvision(AnyTypeKind.USER.name()).getMapping());
    }

    @Test
    public void issueSYNCOPE368() {
        final String name = "SYNCOPE368-" + getUUIDString();

        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(name);
        resourceTO.setConnector(105L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.GROUP.name());
        provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.GroupName);
        item.setExtAttrName("cn");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.GroupOwnerSchema);
        item.setExtAttrName("owner");
        item.setPurpose(MappingPurpose.BOTH);
        mapping.add(item);

        resourceTO = createResource(resourceTO);
        assertNotNull(resourceTO);
        assertEquals(2, resourceTO.getProvision(AnyTypeKind.GROUP.name()).getMapping().getItems().size());
    }

    @Test
    public void issueSYNCOPE418() {
        try {
            resourceService.create(
                    buildResourceTO("http://schemas.examples.org/security/authorization/organizationUnit"));
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidExternalResource, e.getType());

            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void issueSYNCOPE493() {
        // create resource with attribute mapping set to NONE and check its propagation
        String resourceName = RESOURCE_NAME_CREATE_NONE;
        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resourceName);
        resourceTO.setConnector(102L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntMappingType(IntMappingType.UserKey);
        item.setExtAttrName("userId");
        item.setConnObjectKey(true);
        item.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setConnObjectKeyItem(item);

        MappingItemTO item2 = new MappingItemTO();
        item2.setIntMappingType(IntMappingType.UserPlainSchema);
        item2.setConnObjectKey(false);
        item2.setIntAttrName("gender");
        item2.setExtAttrName("gender");
        item2.setPurpose(MappingPurpose.NONE);
        mapping.add(item2);

        Response response = resourceService.create(resourceTO);
        ResourceTO actual = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).getMapping());
        assertNotNull(actual.getProvision(AnyTypeKind.USER.name()).getMapping().getItems());
        assertEquals(MappingPurpose.PROPAGATION,
                actual.getProvision(AnyTypeKind.USER.name()).getMapping().getConnObjectKeyItem().getPurpose());
        for (MappingItemTO itemTO : actual.getProvision(AnyTypeKind.USER.name()).getMapping().getItems()) {
            if ("gender".equals(itemTO.getIntAttrName())) {
                assertEquals(MappingPurpose.NONE, itemTO.getPurpose());
            }
        }
    }
}
