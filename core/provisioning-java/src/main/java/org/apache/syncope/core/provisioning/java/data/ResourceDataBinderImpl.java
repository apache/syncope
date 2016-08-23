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

import java.util.HashSet;
import java.util.Iterator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.OrgUnitTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceDataBinderImpl implements ResourceDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceDataBinder.class);

    private static final String[] MAPPINGITEM_IGNORE_PROPERTIES = { "key", "mapping" };

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Override
    public ExternalResource create(final ResourceTO resourceTO) {
        return update(entityFactory.newEntity(ExternalResource.class), resourceTO);
    }

    @Override
    public ExternalResource update(final ExternalResource resource, final ResourceTO resourceTO) {
        if (resourceTO == null) {
            return null;
        }

        resource.setKey(resourceTO.getKey());

        if (resourceTO.getConnector() != null) {
            ConnInstance connector = connInstanceDAO.find(resourceTO.getConnector());
            resource.setConnector(connector);

            if (!connector.getResources().contains(resource)) {
                connector.add(resource);
            }
        }

        resource.setEnforceMandatoryCondition(resourceTO.isEnforceMandatoryCondition());

        resource.setPropagationPriority(resourceTO.getPropagationPriority());

        resource.setRandomPwdIfNotProvided(resourceTO.isRandomPwdIfNotProvided());

        // 1. add or update all (valid) provisions from TO
        for (ProvisionTO provisionTO : resourceTO.getProvisions()) {
            AnyType anyType = anyTypeDAO.find(provisionTO.getAnyType());
            if (anyType == null) {
                LOG.debug("Invalid {} specified {}, ignoring...",
                        AnyType.class.getSimpleName(), provisionTO.getAnyType());
            } else {
                Provision provision = resource.getProvision(anyType);
                if (provision == null) {
                    provision = entityFactory.newEntity(Provision.class);
                    provision.setResource(resource);
                    resource.add(provision);
                    provision.setAnyType(anyType);
                }

                if (provisionTO.getObjectClass() == null) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidProvision);
                    sce.getElements().add("Null " + ObjectClass.class.getSimpleName());
                    throw sce;
                }
                provision.setObjectClass(new ObjectClass(provisionTO.getObjectClass()));

                // add all classes contained in the TO
                for (String name : provisionTO.getAuxClasses()) {
                    AnyTypeClass anyTypeClass = anyTypeClassDAO.find(name);
                    if (anyTypeClass == null) {
                        LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), name);
                    } else {
                        provision.add(anyTypeClass);
                    }
                }
                // remove all classes not contained in the TO
                for (Iterator<? extends AnyTypeClass> itor = provision.getAuxClasses().iterator(); itor.hasNext();) {
                    AnyTypeClass anyTypeClass = itor.next();
                    if (!provisionTO.getAuxClasses().contains(anyTypeClass.getKey())) {
                        itor.remove();
                    }
                }

                if (provisionTO.getMapping() == null) {
                    provision.setMapping(null);
                } else {
                    Mapping mapping = provision.getMapping();
                    if (mapping == null) {
                        mapping = entityFactory.newEntity(Mapping.class);
                        mapping.setProvision(provision);
                        provision.setMapping(mapping);
                    } else {
                        mapping.getItems().clear();
                    }

                    AnyTypeClassTO allowedSchemas = new AnyTypeClassTO();
                    for (Iterator<AnyTypeClass> itor = IteratorUtils.chainedIterator(
                            provision.getAnyType().getClasses().iterator(),
                            provision.getAuxClasses().iterator()); itor.hasNext();) {

                        AnyTypeClass anyTypeClass = itor.next();
                        allowedSchemas.getPlainSchemas().addAll(
                                CollectionUtils.collect(anyTypeClass.getPlainSchemas(),
                                        EntityUtils.<PlainSchema>keyTransformer()));
                        allowedSchemas.getDerSchemas().addAll(
                                CollectionUtils.collect(anyTypeClass.getDerSchemas(),
                                        EntityUtils.<DerSchema>keyTransformer()));
                        allowedSchemas.getVirSchemas().addAll(
                                CollectionUtils.collect(anyTypeClass.getVirSchemas(),
                                        EntityUtils.<VirSchema>keyTransformer()));
                    }

                    populateMapping(
                            provisionTO.getMapping(),
                            mapping,
                            allowedSchemas);
                }

                if (provisionTO.getVirSchemas().isEmpty()) {
                    for (VirSchema schema : virSchemaDAO.findByProvision(provision)) {
                        virSchemaDAO.delete(schema.getKey());
                    }
                } else {
                    for (String schemaName : provisionTO.getVirSchemas()) {
                        VirSchema schema = virSchemaDAO.find(schemaName);
                        if (schema == null) {
                            LOG.debug("Invalid {} specified: {}, ignoring...",
                                    VirSchema.class.getSimpleName(), schemaName);
                        } else {
                            schema.setProvision(provision);
                        }
                    }
                }
            }
        }

        // 2. remove all provisions not contained in the TO
        for (Iterator<? extends Provision> itor = resource.getProvisions().iterator(); itor.hasNext();) {
            Provision provision = itor.next();
            if (resourceTO.getProvision(provision.getAnyType().getKey()) == null) {
                for (VirSchema schema : virSchemaDAO.findByProvision(provision)) {
                    virSchemaDAO.delete(schema.getKey());
                }

                itor.remove();
            }
        }

        // 3. orgUnit
        if (resourceTO.getOrgUnit() == null && resource.getOrgUnit() != null) {
            resource.getOrgUnit().setResource(null);
            resource.setOrgUnit(null);
        } else if (resourceTO.getOrgUnit() != null) {
            OrgUnitTO orgUnitTO = resourceTO.getOrgUnit();

            OrgUnit orgUnit = resource.getOrgUnit();
            if (orgUnit == null) {
                orgUnit = entityFactory.newEntity(OrgUnit.class);
                orgUnit.setResource(resource);
                resource.setOrgUnit(orgUnit);
            }

            if (orgUnitTO.getObjectClass() == null) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidOrgUnit);
                sce.getElements().add("Null " + ObjectClass.class.getSimpleName());
                throw sce;
            }
            orgUnit.setObjectClass(new ObjectClass(orgUnitTO.getObjectClass()));

            if (orgUnitTO.getExtAttrName() == null) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidOrgUnit);
                sce.getElements().add("Null extAttrName");
                throw sce;
            }
            orgUnit.setExtAttrName(orgUnitTO.getExtAttrName());

            if (orgUnitTO.getConnObjectLink() == null) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidOrgUnit);
                sce.getElements().add("Null connObjectLink");
                throw sce;
            }
            orgUnit.setConnObjectLink(orgUnitTO.getConnObjectLink());
        }

        resource.setCreateTraceLevel(resourceTO.getCreateTraceLevel());
        resource.setUpdateTraceLevel(resourceTO.getUpdateTraceLevel());
        resource.setDeleteTraceLevel(resourceTO.getDeleteTraceLevel());
        resource.setProvisioningTraceLevel(resourceTO.getProvisioningTraceLevel());

        resource.setPasswordPolicy(resourceTO.getPasswordPolicy() == null
                ? null : (PasswordPolicy) policyDAO.find(resourceTO.getPasswordPolicy()));

        resource.setAccountPolicy(resourceTO.getAccountPolicy() == null
                ? null : (AccountPolicy) policyDAO.find(resourceTO.getAccountPolicy()));

        resource.setPullPolicy(resourceTO.getPullPolicy() == null
                ? null : (PullPolicy) policyDAO.find(resourceTO.getPullPolicy()));

        resource.setConfOverride(new HashSet<>(resourceTO.getConfOverride()));

        resource.setOverrideCapabilities(resourceTO.isOverrideCapabilities());
        resource.getCapabilitiesOverride().clear();
        resource.getCapabilitiesOverride().addAll(resourceTO.getCapabilitiesOverride());

        resource.getPropagationActionsClassNames().clear();
        resource.getPropagationActionsClassNames().addAll(resourceTO.getPropagationActionsClassNames());

        return resource;
    }

    private void populateMapping(
            final MappingTO mappingTO,
            final Mapping mapping,
            final AnyTypeClassTO allowedSchemas) {

        mapping.setConnObjectLink(mappingTO.getConnObjectLink());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping = SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing = SyncopeClientException.build(
                ClientExceptionType.RequiredValuesMissing);

        for (MappingItemTO itemTO : mappingTO.getItems()) {
            if (itemTO == null) {
                LOG.error("Null {}", MappingItemTO.class.getSimpleName());
                invalidMapping.getElements().add("Null " + MappingItemTO.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                IntAttrName intAttrName = intAttrNameParser.parse(
                        itemTO.getIntAttrName(),
                        mapping.getProvision().getAnyType().getKind());

                if (intAttrName.getSchemaType() == null && intAttrName.getField() == null) {
                    LOG.error("'{}' not existing", itemTO.getIntAttrName());
                    invalidMapping.getElements().add("'" + itemTO.getIntAttrName() + "' not existing");
                } else {
                    boolean allowed = true;
                    if (intAttrName.getSchemaType() != null
                            && intAttrName.getEnclosingGroup() == null
                            && intAttrName.getRelatedAnyObject() == null) {
                        switch (intAttrName.getSchemaType()) {
                            case PLAIN:
                                allowed = allowedSchemas.getPlainSchemas().contains(intAttrName.getSchemaName());
                                break;

                            case DERIVED:
                                allowed = allowedSchemas.getDerSchemas().contains(intAttrName.getSchemaName());
                                break;

                            case VIRTUAL:
                                allowed = allowedSchemas.getVirSchemas().contains(intAttrName.getSchemaName());
                                break;

                            default:
                        }
                    }

                    if (allowed) {
                        // no mandatory condition implies mandatory condition false
                        if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                                ? "false" : itemTO.getMandatoryCondition())) {

                            SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                                    ClientExceptionType.InvalidValues);
                            invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                            scce.addException(invalidMandatoryCondition);
                        }

                        MappingItem item = entityFactory.newEntity(MappingItem.class);
                        BeanUtils.copyProperties(itemTO, item, MAPPINGITEM_IGNORE_PROPERTIES);
                        item.setMapping(mapping);
                        if (item.isConnObjectKey()) {
                            if (intAttrName.getSchemaType() == SchemaType.VIRTUAL) {
                                invalidMapping.getElements().
                                        add("Virtual attributes cannot be set as ConnObjectKey");
                            }
                            if ("password".equals(intAttrName.getField())) {
                                invalidMapping.getElements().add(
                                        "Password attributes cannot be set as ConnObjectKey");
                            }

                            mapping.setConnObjectKeyItem(item);
                        } else {
                            mapping.add(item);
                        }

                        if (intAttrName.getEnclosingGroup() != null
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name()
                                    + " allowed when referring to groups");
                        }
                        if (intAttrName.getRelatedAnyObject() != null
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name()
                                    + " allowed when referring to any objects");
                        }
                        if (intAttrName.getSchemaType() == SchemaType.DERIVED
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name() + " allowed for derived");
                        }
                        if (intAttrName.getSchemaType() == SchemaType.VIRTUAL) {
                            if (item.getPurpose() != MappingPurpose.PROPAGATION) {
                                invalidMapping.getElements().add(
                                        "Only " + MappingPurpose.PROPAGATION.name() + " allowed for virtual");
                            }

                            VirSchema schema = virSchemaDAO.find(item.getIntAttrName());
                            if (schema != null && schema.getProvision().equals(item.getMapping().getProvision())) {
                                invalidMapping.getElements().add(
                                        "No need to map virtual schema on linking resource");
                            }
                        }
                    } else {
                        LOG.error("'{}' not allowed", itemTO.getIntAttrName());
                        invalidMapping.getElements().add("'" + itemTO.getIntAttrName() + "' not allowed");
                    }
                }
            }
        }

        if (!invalidMapping.getElements().isEmpty()) {
            scce.addException(invalidMapping);
        }
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    private void populateMappingTO(final Mapping mapping, final MappingTO mappingTO) {
        mappingTO.setConnObjectLink(mapping.getConnObjectLink());

        for (MappingItem item : mapping.getItems()) {
            MappingItemTO itemTO = new MappingItemTO();
            itemTO.setKey(item.getKey());
            BeanUtils.copyProperties(item, itemTO, MAPPINGITEM_IGNORE_PROPERTIES);

            if (itemTO.isConnObjectKey()) {
                mappingTO.setConnObjectKeyItem(itemTO);
            } else {
                mappingTO.add(itemTO);
            }
        }
    }

    @Override
    public ResourceTO getResourceTO(final ExternalResource resource) {
        ResourceTO resourceTO = new ResourceTO();

        // set the resource name
        resourceTO.setKey(resource.getKey());

        // set the connector instance
        ConnInstance connector = resource.getConnector();

        resourceTO.setConnector(connector == null ? null : connector.getKey());
        resourceTO.setConnectorDisplayName(connector == null ? null : connector.getDisplayName());

        // set the provision information
        for (Provision provision : resource.getProvisions()) {
            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setKey(provision.getKey());
            provisionTO.setAnyType(provision.getAnyType().getKey());
            provisionTO.setObjectClass(provision.getObjectClass().getObjectClassValue());
            provisionTO.getAuxClasses().addAll(CollectionUtils.collect(
                    provision.getAuxClasses(), EntityUtils.<AnyTypeClass>keyTransformer()));
            provisionTO.setSyncToken(provision.getSerializedSyncToken());

            if (provision.getMapping() != null) {
                MappingTO mappingTO = new MappingTO();
                provisionTO.setMapping(mappingTO);
                populateMappingTO(provision.getMapping(), mappingTO);
            }

            for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
                provisionTO.getVirSchemas().add(virSchema.getKey());

                MappingItem linkingMappingItem = virSchema.asLinkingMappingItem();

                MappingItemTO itemTO = new MappingItemTO();
                itemTO.setKey(linkingMappingItem.getKey());
                BeanUtils.copyProperties(linkingMappingItem, itemTO, MAPPINGITEM_IGNORE_PROPERTIES);

                provisionTO.getMapping().getLinkingItems().add(itemTO);
            }

            resourceTO.getProvisions().add(provisionTO);
        }

        if (resource.getOrgUnit() != null) {
            OrgUnit orgUnit = resource.getOrgUnit();

            OrgUnitTO orgUnitTO = new OrgUnitTO();
            orgUnitTO.setKey(orgUnit.getKey());
            orgUnitTO.setObjectClass(orgUnit.getObjectClass().getObjectClassValue());
            orgUnitTO.setSyncToken(orgUnit.getSerializedSyncToken());
            orgUnitTO.setExtAttrName(orgUnit.getExtAttrName());
            orgUnitTO.setConnObjectLink(orgUnit.getConnObjectLink());

            resourceTO.setOrgUnit(orgUnitTO);
        }

        resourceTO.setEnforceMandatoryCondition(resource.isEnforceMandatoryCondition());

        resourceTO.setPropagationPriority(resource.getPropagationPriority());

        resourceTO.setRandomPwdIfNotProvided(resource.isRandomPwdIfNotProvided());

        resourceTO.setCreateTraceLevel(resource.getCreateTraceLevel());
        resourceTO.setUpdateTraceLevel(resource.getUpdateTraceLevel());
        resourceTO.setDeleteTraceLevel(resource.getDeleteTraceLevel());
        resourceTO.setProvisioningTraceLevel(resource.getProvisioningTraceLevel());

        resourceTO.setPasswordPolicy(resource.getPasswordPolicy() == null
                ? null : resource.getPasswordPolicy().getKey());

        resourceTO.setAccountPolicy(resource.getAccountPolicy() == null
                ? null : resource.getAccountPolicy().getKey());

        resourceTO.setPullPolicy(resource.getPullPolicy() == null
                ? null : resource.getPullPolicy().getKey());

        resourceTO.getConfOverride().addAll(resource.getConfOverride());

        resourceTO.setOverrideCapabilities(resource.isOverrideCapabilities());
        resourceTO.getCapabilitiesOverride().addAll(resource.getCapabilitiesOverride());

        resourceTO.getPropagationActionsClassNames().addAll(resource.getPropagationActionsClassNames());

        return resourceTO;
    }
}
