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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResourceDataBinderTest extends AbstractDataBinderTest {

    private static final String CONNECTOR_KEY = "88a7a819-dab5-46b4-9b90-0b9769eabdb8";

    private static final String PASSWORD_POLICY_KEY = "ce93fcda-dc3a-4369-a7b0-a6108c261c85";

    private static final String ACCOUNT_POLICY_KEY = "20ab5a8c-4b0c-432c-b957-f7fb9784d9f7";

    private static final String PROPAGATION_POLICY_KEY = "89d322db-9878-420c-b49c-67be13df9a12";

    private static final String INBOUND_POLICY_KEY = "66691e96-285f-4464-bc19-e68384ea4c85";

    private static final String PUSH_POLICY_KEY = "fb6530e5-892d-4f47-a46b-180c5b6c5c83";

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder binder;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private EntityFactory entityFactory;

    protected Implementation provisionSorter() {
        Implementation provisionSorter = entityFactory.newEntity(Implementation.class);
        provisionSorter.setKey("TestProvisionSorter");
        provisionSorter.setEngine(ImplementationEngine.JAVA);
        provisionSorter.setType(IdMImplementationType.PROVISION_SORTER);
        provisionSorter.setBody("org.apache.syncope.core.provisioning.java.pushpull.DefaultProvisionSorter");
        return implementationDAO.save(provisionSorter);
    }

    protected Item item(final String intAttrName, final String extAttrName, final MappingPurpose purpose) {
        Item item = new Item();
        item.setIntAttrName(intAttrName);
        item.setExtAttrName(extAttrName);
        item.setPurpose(purpose);
        return item;
    }

    protected ConnConfProperty confProperty(final String name, final Object value) {
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setName(name);
        schema.setType(value.getClass().getName());
        schema.setRequired(true);
        schema.setOrder(1);

        ConnConfProperty property = new ConnConfProperty();
        property.setSchema(schema);
        property.setOverridable(true);
        property.getValues().add(value);
        return property;
    }

    @Test
    void issue42() {
        PlainSchema userId = plainSchemaDAO.findById("userId").orElseThrow();

        Set<Item> beforeUserIdMappings = resourceDAO.findAll().stream().
                map(r -> r.getProvisionByAnyType(org.apache.syncope.common.lib.types.AnyTypeKind.USER.name())).
                flatMap(Optional::stream).
                flatMap(p -> p.getMapping().getItems().stream()).
                filter(i -> userId.getKey().equals(i.getIntAttrName())).
                collect(Collectors.toSet());

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey("resource-issue42");
        resourceTO.setConnector(CONNECTOR_KEY);
        resourceTO.setEnforceMandatoryCondition(true);

        Provision provisionTO = new Provision();
        provisionTO.setAnyType(org.apache.syncope.common.lib.types.AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        Mapping mapping = new Mapping();
        provisionTO.setMapping(mapping);

        Item item = item("userId", "campo1", MappingPurpose.BOTH);
        item.setConnObjectKey(true);
        item.setMandatoryCondition("false");
        mapping.setConnObjectKeyItem(item);

        ExternalResource resource = binder.create(resourceTO);
        resource = resourceDAO.save(resource);
        entityManager.flush();

        ExternalResource actual = resourceDAO.findById("resource-issue42").orElseThrow();
        assertEquals(resource, actual);
        assertEquals(1, actual.getProvisionByAnyType(
                org.apache.syncope.common.lib.types.AnyTypeKind.USER.name()).orElseThrow().
                getMapping().getItems().size());

        Set<Item> afterUserIdMappings = resourceDAO.findAll().stream().
                map(r -> r.getProvisionByAnyType(org.apache.syncope.common.lib.types.AnyTypeKind.USER.name())).
                flatMap(Optional::stream).
                flatMap(p -> p.getMapping().getItems().stream()).
                filter(i -> userId.getKey().equals(i.getIntAttrName())).
                collect(Collectors.toSet());

        assertEquals(beforeUserIdMappings.size(), afterUserIdMappings.size() - 1);
    }

    @Test
    void createFull() {
        Implementation provisionSorter = provisionSorter();

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey("resource-full");
        resourceTO.setConnector(CONNECTOR_KEY);
        resourceTO.setEnforceMandatoryCondition(true);
        resourceTO.setPropagationPriority(3);
        resourceTO.setCreateTraceLevel(TraceLevel.FAILURES);
        resourceTO.setUpdateTraceLevel(TraceLevel.SUMMARY);
        resourceTO.setDeleteTraceLevel(TraceLevel.NONE);
        resourceTO.setProvisioningTraceLevel(TraceLevel.ALL);
        resourceTO.setPasswordPolicy(PASSWORD_POLICY_KEY);
        resourceTO.setAccountPolicy(ACCOUNT_POLICY_KEY);
        resourceTO.setPropagationPolicy(PROPAGATION_POLICY_KEY);
        resourceTO.setInboundPolicy(INBOUND_POLICY_KEY);
        resourceTO.setPushPolicy(PUSH_POLICY_KEY);
        resourceTO.setProvisionSorter(provisionSorter.getKey());
        resourceTO.setConfOverride(Optional.of(List.of(confProperty("endpoint", "http://localhost:8080/test"))));
        resourceTO.setCapabilitiesOverride(Optional.of(Set.of(
                ConnectorCapability.CREATE, ConnectorCapability.UPDATE, ConnectorCapability.SEARCH)));
        resourceTO.getPropagationActions().add("GenerateRandomPasswordPropagationActions");
        resourceTO.getPropagationActions().add("not-existing");

        Provision userProvision = new Provision();
        userProvision.setAnyType(AnyTypeKind.USER.name());
        userProvision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        userProvision.getAuxClasses().add("csv");
        userProvision.getAuxClasses().add("not-existing");
        userProvision.setIgnoreCaseMatch(true);
        userProvision.setUidOnCreate("userId");

        Mapping userMapping = new Mapping();
        userMapping.setConnObjectLink("userId");
        userProvision.setMapping(userMapping);

        Item userKey = item("username", "userId", MappingPurpose.BOTH);
        userMapping.setConnObjectKeyItem(userKey);

        Item email = item("email", "email", MappingPurpose.BOTH);
        userMapping.add(email);

        Item password = item("password", "__PASSWORD__", MappingPurpose.BOTH);
        password.setPassword(true);
        userMapping.add(password);

        Item ctype = item("ctype", "ctype", MappingPurpose.BOTH);
        userMapping.add(ctype);

        Item cn = item("cn", "cn", MappingPurpose.PROPAGATION);
        cn.setPropagationJEXLTransformer("surname + ', ' + firstname");
        cn.setPullJEXLTransformer("surname");
        cn.getTransformers().add("not-existing");
        userMapping.add(cn);

        resourceTO.getProvisions().add(userProvision);

        Provision printerProvision = new Provision();
        printerProvision.setAnyType("PRINTER");
        printerProvision.setObjectClass(ObjectClass.GROUP_NAME);
        printerProvision.getAuxClasses().add("csv");
        printerProvision.setUidOnCreate("not-existing");

        Mapping printerMapping = new Mapping();
        printerMapping.setConnObjectLink("model");
        printerProvision.setMapping(printerMapping);

        Item printerKey = item("model", "model", MappingPurpose.BOTH);
        printerMapping.setConnObjectKeyItem(printerKey);
        printerMapping.add(item("location", "location", MappingPurpose.BOTH));
        printerMapping.add(item("csvuserid", "csvuserid", MappingPurpose.PROPAGATION));

        resourceTO.getProvisions().add(printerProvision);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setObjectClass(ObjectClass.GROUP_NAME);
        orgUnit.setConnObjectLink("cn=groups,dc=example,dc=com");
        orgUnit.setIgnoreCaseMatch(true);

        Item orgUnitKey = item("key", "cn", MappingPurpose.BOTH);
        orgUnit.setConnObjectKeyItem(orgUnitKey);
        orgUnit.add(item("name", "description", MappingPurpose.BOTH));
        resourceTO.setOrgUnit(orgUnit);

        ExternalResource resource = binder.create(resourceTO);
        resourceDAO.save(resource);
        entityManager.flush();

        ExternalResource actual = resourceDAO.findById("resource-full").orElseThrow();
        assertEquals(CONNECTOR_KEY, actual.getConnector().getKey());
        assertTrue(actual.isEnforceMandatoryCondition());
        assertEquals(3, actual.getPropagationPriority());
        assertEquals(TraceLevel.FAILURES, actual.getCreateTraceLevel());
        assertEquals(TraceLevel.SUMMARY, actual.getUpdateTraceLevel());
        assertEquals(TraceLevel.NONE, actual.getDeleteTraceLevel());
        assertEquals(TraceLevel.ALL, actual.getProvisioningTraceLevel());
        assertEquals(PASSWORD_POLICY_KEY, actual.getPasswordPolicy().getKey());
        assertEquals(ACCOUNT_POLICY_KEY, actual.getAccountPolicy().getKey());
        assertEquals(PROPAGATION_POLICY_KEY, actual.getPropagationPolicy().getKey());
        assertEquals(INBOUND_POLICY_KEY, actual.getInboundPolicy().getKey());
        assertEquals(PUSH_POLICY_KEY, actual.getPushPolicy().getKey());
        assertEquals(provisionSorter.getKey(), actual.getProvisionSorter().getKey());
        assertEquals("endpoint",
                actual.getConfOverride().orElseThrow().getFirst().getSchema().getName());
        assertEquals(Set.of(
                ConnectorCapability.CREATE, ConnectorCapability.UPDATE, ConnectorCapability.SEARCH),
                actual.getCapabilitiesOverride().orElseThrow());
        assertEquals(List.of("GenerateRandomPasswordPropagationActions"),
                actual.getPropagationActions().stream().map(Implementation::getKey).toList());

        Provision userProvisionActual = actual.getProvisionByAnyType(AnyTypeKind.USER.name()).orElseThrow();
        assertEquals(ObjectClass.ACCOUNT_NAME, userProvisionActual.getObjectClass());
        assertEquals(List.of("csv"), userProvisionActual.getAuxClasses());
        assertTrue(userProvisionActual.isIgnoreCaseMatch());
        assertEquals("userId", userProvisionActual.getUidOnCreate());
        assertEquals("username", userProvisionActual.getMapping().getConnObjectKeyItem().
                orElseThrow().getIntAttrName());
        assertEquals(5, userProvisionActual.getMapping().getItems().size());
        Item cnActual = userProvisionActual.getMapping().getItems().stream().
                filter(i -> "cn".equals(i.getIntAttrName())).
                findFirst().orElseThrow();
        assertEquals(MappingPurpose.PROPAGATION, cnActual.getPurpose());
        assertEquals("surname + ', ' + firstname", cnActual.getPropagationJEXLTransformer());
        assertEquals("surname", cnActual.getPullJEXLTransformer());
        assertTrue(cnActual.getTransformers().isEmpty());

        Provision printerProvisionActual = actual.getProvisionByAnyType("PRINTER").orElseThrow();
        assertEquals(ObjectClass.GROUP_NAME, printerProvisionActual.getObjectClass());
        assertEquals(List.of("csv"), printerProvisionActual.getAuxClasses());
        assertNull(printerProvisionActual.getUidOnCreate());
        assertEquals("model", printerProvisionActual.getMapping().getConnObjectKeyItem().
                orElseThrow().getIntAttrName());

        assertNotNull(actual.getOrgUnit());
        assertEquals(ObjectClass.GROUP_NAME, actual.getOrgUnit().getObjectClass());
        assertEquals("cn=groups,dc=example,dc=com", actual.getOrgUnit().getConnObjectLink());
        assertTrue(actual.getOrgUnit().isIgnoreCaseMatch());
        assertEquals("key", actual.getOrgUnit().getConnObjectKeyItem().orElseThrow().getIntAttrName());

        ResourceTO actualTO = binder.getResourceTO(actual);
        assertEquals("resource-full", actualTO.getKey());
        assertEquals(CONNECTOR_KEY, actualTO.getConnector());
        assertEquals(2, actualTO.getProvisions().size());
        assertNotNull(actualTO.getOrgUnit());
        assertEquals(provisionSorter.getKey(), actualTO.getProvisionSorter());
        assertFalse(actualTO.getPropagationActions().isEmpty());
    }

    @Test
    void updateFull() {
        Implementation provisionSorter = provisionSorter();

        ExternalResource resource = resourceDAO.findById("ws-target-resource-1").orElseThrow();

        ResourceTO resourceTO = new ResourceTO();
        resourceTO.setKey(resource.getKey());
        resourceTO.setConnector(CONNECTOR_KEY);
        resourceTO.setEnforceMandatoryCondition(true);
        resourceTO.setPropagationPriority(2);
        resourceTO.setCreateTraceLevel(TraceLevel.SUMMARY);
        resourceTO.setUpdateTraceLevel(TraceLevel.FAILURES);
        resourceTO.setDeleteTraceLevel(TraceLevel.ALL);
        resourceTO.setProvisioningTraceLevel(TraceLevel.NONE);
        resourceTO.setPasswordPolicy(PASSWORD_POLICY_KEY);
        resourceTO.setAccountPolicy(ACCOUNT_POLICY_KEY);
        resourceTO.setPropagationPolicy(PROPAGATION_POLICY_KEY);
        resourceTO.setInboundPolicy(INBOUND_POLICY_KEY);
        resourceTO.setPushPolicy(PUSH_POLICY_KEY);
        resourceTO.setProvisionSorter(provisionSorter.getKey());
        resourceTO.setConfOverride(Optional.of(List.of(confProperty("endpoint", "http://localhost:8081/test"))));
        resourceTO.setCapabilitiesOverride(Optional.of(Set.of(ConnectorCapability.UPDATE)));
        resourceTO.getPropagationActions().add("GenerateRandomPasswordPropagationActions");

        Provision userProvision = new Provision();
        userProvision.setAnyType(AnyTypeKind.USER.name());
        userProvision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        userProvision.getAuxClasses().add("csv");
        userProvision.setIgnoreCaseMatch(true);
        userProvision.setUidOnCreate("userId");

        Mapping userMapping = new Mapping();
        userMapping.setConnObjectLink("userId");
        userProvision.setMapping(userMapping);
        userMapping.setConnObjectKeyItem(item("username", "userId", MappingPurpose.BOTH));
        userMapping.add(item("email", "email", MappingPurpose.BOTH));
        Item password = item("password", "__PASSWORD__", MappingPurpose.BOTH);
        password.setPassword(true);
        userMapping.add(password);
        userMapping.add(item("cn", "cn", MappingPurpose.PROPAGATION));

        resourceTO.getProvisions().add(userProvision);

        Provision printerProvision = new Provision();
        printerProvision.setAnyType("PRINTER");
        printerProvision.setObjectClass(ObjectClass.GROUP_NAME);
        Mapping printerMapping = new Mapping();
        printerProvision.setMapping(printerMapping);
        printerMapping.setConnObjectKeyItem(item("model", "model", MappingPurpose.BOTH));
        printerMapping.add(item("location", "location", MappingPurpose.BOTH));
        resourceTO.getProvisions().add(printerProvision);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setObjectClass(ObjectClass.GROUP_NAME);
        orgUnit.setConnObjectLink("cn=units,dc=example,dc=com");
        orgUnit.setConnObjectKeyItem(item("key", "cn", MappingPurpose.BOTH));
        resourceTO.setOrgUnit(orgUnit);

        binder.update(resource, resourceTO);
        entityManager.flush();

        ExternalResource actual = resourceDAO.findById(resource.getKey()).orElseThrow();
        assertTrue(actual.isEnforceMandatoryCondition());
        assertEquals(2, actual.getPropagationPriority());
        assertEquals(TraceLevel.SUMMARY, actual.getCreateTraceLevel());
        assertEquals(TraceLevel.FAILURES, actual.getUpdateTraceLevel());
        assertEquals(TraceLevel.ALL, actual.getDeleteTraceLevel());
        assertEquals(TraceLevel.NONE, actual.getProvisioningTraceLevel());
        assertEquals(PASSWORD_POLICY_KEY, actual.getPasswordPolicy().getKey());
        assertEquals(ACCOUNT_POLICY_KEY, actual.getAccountPolicy().getKey());
        assertEquals(PROPAGATION_POLICY_KEY, actual.getPropagationPolicy().getKey());
        assertEquals(INBOUND_POLICY_KEY, actual.getInboundPolicy().getKey());
        assertEquals(PUSH_POLICY_KEY, actual.getPushPolicy().getKey());
        assertEquals(provisionSorter.getKey(), actual.getProvisionSorter().getKey());
        assertEquals("http://localhost:8081/test",
                actual.getConfOverride().orElseThrow().getFirst().getValues().getFirst());
        assertEquals(Set.of(ConnectorCapability.UPDATE), actual.getCapabilitiesOverride().orElseThrow());
        assertEquals(1, actual.getPropagationActions().size());

        Provision userProvisionActual = actual.getProvisionByAnyType(AnyTypeKind.USER.name()).orElseThrow();
        assertEquals(List.of("csv"), userProvisionActual.getAuxClasses());
        assertEquals("userId", userProvisionActual.getUidOnCreate());
        assertEquals(4, userProvisionActual.getMapping().getItems().size());
        assertNotNull(actual.getProvisionByAnyType("PRINTER").orElseThrow());
        assertNotNull(actual.getOrgUnit());
        assertEquals("cn=units,dc=example,dc=com", actual.getOrgUnit().getConnObjectLink());

        // now remove org unit, provisions and overrides
        ResourceTO cleanup = new ResourceTO();
        cleanup.setKey(actual.getKey());
        cleanup.setConnector(CONNECTOR_KEY);
        cleanup.setCreateTraceLevel(TraceLevel.ALL);
        cleanup.setUpdateTraceLevel(TraceLevel.ALL);
        cleanup.setDeleteTraceLevel(TraceLevel.ALL);
        cleanup.setProvisioningTraceLevel(TraceLevel.ALL);
        cleanup.setConfOverride(Optional.empty());
        cleanup.setCapabilitiesOverride(Optional.empty());

        binder.update(actual, cleanup);
        entityManager.flush();

        ExternalResource cleaned = resourceDAO.findById(actual.getKey()).orElseThrow();
        assertNull(cleaned.getOrgUnit());
        assertTrue(cleaned.getProvisions().isEmpty());
        assertTrue(cleaned.getConfOverride().isEmpty());
        assertTrue(cleaned.getCapabilitiesOverride().isEmpty());
        assertTrue(cleaned.getPropagationActions().isEmpty());
        assertNull(cleaned.getProvisionSorter());
        assertNull(cleaned.getPasswordPolicy());
        assertNull(cleaned.getAccountPolicy());
        assertNull(cleaned.getPropagationPolicy());
        assertNull(cleaned.getInboundPolicy());
        assertNull(cleaned.getPushPolicy());
    }

    @Test
    void invalid() {
        ResourceTO nullObjectClass = new ResourceTO();
        nullObjectClass.setKey("resource-invalid");
        nullObjectClass.setConnector(CONNECTOR_KEY);
        Provision provision = new Provision();
        provision.setAnyType(AnyTypeKind.USER.name());
        nullObjectClass.getProvisions().add(provision);
        assertThrows(SyncopeClientException.class, () -> binder.create(nullObjectClass));

        ResourceTO nullIntAttrName = new ResourceTO();
        nullIntAttrName.setKey("resource-invalid");
        nullIntAttrName.setConnector(CONNECTOR_KEY);
        Provision provisionTO = new Provision();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        provisionTO.setMapping(new Mapping());
        provisionTO.getMapping().setConnObjectKeyItem(new Item());
        nullIntAttrName.getProvisions().add(provisionTO);
        assertThrows(SyncopeClientException.class, () -> binder.create(nullIntAttrName));

        ResourceTO wrongDerivedPurpose = new ResourceTO();
        wrongDerivedPurpose.setKey("resource-invalid");
        wrongDerivedPurpose.setConnector(CONNECTOR_KEY);
        Provision wrongDerivedPurposeProvision = new Provision();
        wrongDerivedPurposeProvision.setAnyType(AnyTypeKind.USER.name());
        wrongDerivedPurposeProvision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        wrongDerivedPurposeProvision.setMapping(new Mapping());
        wrongDerivedPurposeProvision.getMapping().setConnObjectKeyItem(
                item("username", "userId", MappingPurpose.BOTH));
        wrongDerivedPurposeProvision.getMapping().add(
                item("cn", "cn", MappingPurpose.BOTH));
        wrongDerivedPurpose.getProvisions().add(wrongDerivedPurposeProvision);
        assertThrows(SyncopeClientException.class, () -> binder.create(wrongDerivedPurpose));

        ResourceTO invalidOrgUnit = new ResourceTO();
        invalidOrgUnit.setKey("resource-invalid");
        invalidOrgUnit.setConnector(CONNECTOR_KEY);
        invalidOrgUnit.setOrgUnit(new OrgUnit());
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidOrgUnit));
    }
}
