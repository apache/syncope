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

import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ItemContainerTO;
import org.apache.syncope.common.lib.to.ItemTO;
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
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceDataBinderImpl implements ResourceDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ResourceDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final ConnInstanceDAO connInstanceDAO;

    protected final PolicyDAO policyDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final ImplementationDAO implementationDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final EntityFactory entityFactory;

    protected final IntAttrNameParser intAttrNameParser;

    protected final PropagationTaskExecutor propagationTaskExecutor;

    public ResourceDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final ConnInstanceDAO connInstanceDAO,
            final PolicyDAO policyDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ImplementationDAO implementationDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser,
            final PropagationTaskExecutor propagationTaskExecutor) {

        this.anyTypeDAO = anyTypeDAO;
        this.connInstanceDAO = connInstanceDAO;
        this.policyDAO = policyDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.implementationDAO = implementationDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.entityFactory = entityFactory;
        this.intAttrNameParser = intAttrNameParser;
        this.propagationTaskExecutor = propagationTaskExecutor;
    }

    @Override
    public ExternalResource create(final ResourceTO resourceTO) {
        return update(entityFactory.newEntity(ExternalResource.class), resourceTO);
    }

    @Override
    public ExternalResource update(final ExternalResource resource, final ResourceTO resourceTO) {
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
        resourceTO.getProvisions().forEach(provisionTO -> {
            AnyType anyType = anyTypeDAO.find(provisionTO.getAnyType());
            if (anyType == null) {
                LOG.debug("Invalid {} specified {}, ignoring...",
                        AnyType.class.getSimpleName(), provisionTO.getAnyType());
            } else {
                Provision provision = resource.getProvision(anyType).orElse(null);
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
                provision.getAuxClasses().
                        removeIf(anyTypeClass -> !provisionTO.getAuxClasses().contains(anyTypeClass.getKey()));

                provision.setIgnoreCaseMatch(provisionTO.isIgnoreCaseMatch());

                if (StringUtils.isBlank(provisionTO.getUidOnCreate())) {
                    provision.setUidOnCreate(null);
                } else {
                    PlainSchema uidOnCreate = plainSchemaDAO.find(provisionTO.getUidOnCreate());
                    if (uidOnCreate == null) {
                        LOG.warn("Ignoring invalid schema for uidOnCreate: {}", provisionTO.getUidOnCreate());
                        provision.setUidOnCreate(null);
                    } else {
                        provision.setUidOnCreate(uidOnCreate);
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
                    Stream.concat(
                            provision.getAnyType().getClasses().stream(),
                            provision.getAuxClasses().stream()).forEach(anyTypeClass -> {

                        allowedSchemas.getPlainSchemas().addAll(anyTypeClass.getPlainSchemas().stream().
                                map(Entity::getKey).collect(Collectors.toList()));
                        allowedSchemas.getDerSchemas().addAll(anyTypeClass.getDerSchemas().stream().
                                map(Entity::getKey).collect(Collectors.toList()));
                        allowedSchemas.getVirSchemas().addAll(anyTypeClass.getVirSchemas().stream().
                                map(Entity::getKey).collect(Collectors.toList()));
                    });

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
        });

        // 2. remove all provisions not contained in the TO
        for (Iterator<? extends Provision> itor = resource.getProvisions().iterator(); itor.hasNext();) {
            Provision provision = itor.next();
            if (resourceTO.getProvision(provision.getAnyType().getKey()).isEmpty()) {
                virSchemaDAO.findByProvision(provision).forEach(schema -> virSchemaDAO.delete(schema.getKey()));

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

            orgUnit.setIgnoreCaseMatch(orgUnitTO.isIgnoreCaseMatch());

            if (orgUnitTO.getConnObjectLink() == null) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidOrgUnit);
                sce.getElements().add("Null connObjectLink");
                throw sce;
            }
            orgUnit.setConnObjectLink(orgUnitTO.getConnObjectLink());

            SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
            SyncopeClientException invalidMapping = SyncopeClientException.build(
                    ClientExceptionType.InvalidMapping);
            SyncopeClientException requiredValuesMissing = SyncopeClientException.build(
                    ClientExceptionType.RequiredValuesMissing);

            orgUnit.getItems().clear();
            for (ItemTO itemTO : orgUnitTO.getItems()) {
                if (itemTO == null) {
                    LOG.error("Null {}", ItemTO.class.getSimpleName());
                    invalidMapping.getElements().add("Null " + ItemTO.class.getSimpleName());
                } else if (itemTO.getIntAttrName() == null) {
                    requiredValuesMissing.getElements().add("intAttrName");
                    scce.addException(requiredValuesMissing);
                } else {
                    if (!"name".equals(itemTO.getIntAttrName()) && !"fullpath".equals(itemTO.getIntAttrName())) {
                        LOG.error("Only 'name' and 'fullpath' are supported for Realms");
                        invalidMapping.getElements().add("Only 'name' and 'fullpath' are supported for Realms");
                    } else {
                        // no mandatory condition implies mandatory condition false
                        if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                                ? "false" : itemTO.getMandatoryCondition())) {

                            SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                                    ClientExceptionType.InvalidValues);
                            invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                            scce.addException(invalidMandatoryCondition);
                        }

                        OrgUnitItem item = entityFactory.newEntity(OrgUnitItem.class);
                        item.setIntAttrName(itemTO.getIntAttrName());
                        item.setExtAttrName(itemTO.getExtAttrName());
                        item.setPurpose(itemTO.getPurpose());
                        item.setMandatoryCondition(itemTO.getMandatoryCondition());
                        item.setConnObjectKey(itemTO.isConnObjectKey());
                        item.setPassword(itemTO.isPassword());
                        item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                        item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                        item.setOrgUnit(orgUnit);
                        if (item.isConnObjectKey()) {
                            orgUnit.setConnObjectKeyItem(item);
                        } else {
                            orgUnit.add(item);
                        }

                        itemTO.getTransformers().forEach(transformerKey -> {
                            Implementation transformer = implementationDAO.find(transformerKey);
                            if (transformer == null) {
                                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...",
                                        transformerKey);
                            } else {
                                item.add(transformer);
                            }
                        });
                        // remove all implementations not contained in the TO
                        item.getTransformers().
                                removeIf(implementation -> !itemTO.getTransformers().contains(implementation.getKey()));
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

        resource.setCreateTraceLevel(resourceTO.getCreateTraceLevel());
        resource.setUpdateTraceLevel(resourceTO.getUpdateTraceLevel());
        resource.setDeleteTraceLevel(resourceTO.getDeleteTraceLevel());
        resource.setProvisioningTraceLevel(resourceTO.getProvisioningTraceLevel());

        resource.setPasswordPolicy(resourceTO.getPasswordPolicy() == null
                ? null : (PasswordPolicy) policyDAO.find(resourceTO.getPasswordPolicy()));

        resource.setAccountPolicy(resourceTO.getAccountPolicy() == null
                ? null : (AccountPolicy) policyDAO.find(resourceTO.getAccountPolicy()));

        if (resource.getPropagationPolicy() != null
                && !resource.getPropagationPolicy().getKey().equals(resourceTO.getPropagationPolicy())) {

            propagationTaskExecutor.expireRetryTemplate(resource.getKey());
        }
        resource.setPropagationPolicy(resourceTO.getPropagationPolicy() == null
                ? null : (PropagationPolicy) policyDAO.find(resourceTO.getPropagationPolicy()));

        resource.setPullPolicy(resourceTO.getPullPolicy() == null
                ? null : (PullPolicy) policyDAO.find(resourceTO.getPullPolicy()));

        resource.setPushPolicy(resourceTO.getPushPolicy() == null
                ? null : (PushPolicy) policyDAO.find(resourceTO.getPushPolicy()));

        if (resourceTO.getProvisionSorter() == null) {
            resource.setProvisionSorter(null);
        } else {
            Implementation provisionSorter = implementationDAO.find(resourceTO.getProvisionSorter());
            if (provisionSorter == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...",
                        resourceTO.getProvisionSorter());
            } else {
                resource.setProvisionSorter(provisionSorter);
            }
        }

        resource.setConfOverride(new HashSet<>(resourceTO.getConfOverride()));

        resource.setOverrideCapabilities(resourceTO.isOverrideCapabilities());
        resource.getCapabilitiesOverride().clear();
        resource.getCapabilitiesOverride().addAll(resourceTO.getCapabilitiesOverride());

        resourceTO.getPropagationActions().forEach(propagationActionKey -> {
            Implementation propagationAction = implementationDAO.find(propagationActionKey);
            if (propagationAction == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", propagationActionKey);
            } else {
                resource.add(propagationAction);
            }
        });
        // remove all implementations not contained in the TO
        resource.getPropagationActions().
                removeIf(implementation -> !resourceTO.getPropagationActions().contains(implementation.getKey()));

        return resource;
    }

    protected void populateMapping(
            final MappingTO mappingTO,
            final Mapping mapping,
            final AnyTypeClassTO allowedSchemas) {

        mapping.setConnObjectLink(mappingTO.getConnObjectLink());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping = SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing = SyncopeClientException.build(
                ClientExceptionType.RequiredValuesMissing);

        for (ItemTO itemTO : mappingTO.getItems()) {
            if (itemTO == null) {
                LOG.error("Null {}", ItemTO.class.getSimpleName());
                invalidMapping.getElements().add("Null " + ItemTO.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                IntAttrName intAttrName = null;
                try {
                    intAttrName = intAttrNameParser.parse(
                            itemTO.getIntAttrName(), mapping.getProvision().getAnyType().getKind());
                } catch (ParseException e) {
                    LOG.error("Invalid intAttrName '{}'", itemTO.getIntAttrName(), e);
                }

                if (intAttrName == null
                        || intAttrName.getSchemaType() == null && intAttrName.getField() == null
                        && intAttrName.getPrivilegesOfApplication() == null) {

                    LOG.error("'{}' not existing", itemTO.getIntAttrName());
                    invalidMapping.getElements().add('\'' + itemTO.getIntAttrName() + "' not existing");
                } else {
                    boolean allowed = true;
                    if (intAttrName.getSchemaType() != null
                            && intAttrName.getEnclosingGroup() == null
                            && intAttrName.getRelatedAnyObject() == null
                            && intAttrName.getRelationshipType() == null
                            && intAttrName.getPrivilegesOfApplication() == null) {

                        switch (intAttrName.getSchemaType()) {
                            case PLAIN:
                                allowed = allowedSchemas.getPlainSchemas().contains(intAttrName.getSchema().getKey());
                                break;

                            case DERIVED:
                                allowed = allowedSchemas.getDerSchemas().contains(intAttrName.getSchema().getKey());
                                break;

                            case VIRTUAL:
                                allowed = allowedSchemas.getVirSchemas().contains(intAttrName.getSchema().getKey());
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
                        item.setIntAttrName(itemTO.getIntAttrName());
                        item.setExtAttrName(itemTO.getExtAttrName());
                        item.setPurpose(itemTO.getPurpose());
                        item.setMandatoryCondition(itemTO.getMandatoryCondition());
                        item.setConnObjectKey(itemTO.isConnObjectKey());
                        item.setPassword(itemTO.isPassword());
                        item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                        item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
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

                        itemTO.getTransformers().forEach(transformerKey -> {
                            Implementation transformer = implementationDAO.find(transformerKey);
                            if (transformer == null) {
                                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...",
                                        transformerKey);
                            } else {
                                item.add(transformer);
                            }
                        });
                        // remove all implementations not contained in the TO
                        item.getTransformers().
                                removeIf(implementation -> !itemTO.getTransformers().contains(implementation.getKey()));

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
                        if (intAttrName.getPrivilegesOfApplication() != null
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name()
                                    + " allowed when referring to privileges");
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
                        if (intAttrName.getRelatedUser() != null
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name()
                                    + " allowed when referring to users");
                        }
                        if ((intAttrName.getRelationshipType() != null
                                || intAttrName.getRelationshipAnyType() != null)
                                && item.getPurpose() != MappingPurpose.PROPAGATION) {

                            invalidMapping.getElements().add(
                                    "Only " + MappingPurpose.PROPAGATION.name()
                                    + " allowed when referring to relationships");
                        }
                    } else {
                        LOG.error("'{}' not allowed", itemTO.getIntAttrName());
                        invalidMapping.getElements().add('\'' + itemTO.getIntAttrName() + "' not allowed");
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

    protected static void populateItems(final List<? extends Item> items, final ItemContainerTO containerTO) {
        items.forEach(item -> {
            ItemTO itemTO = new ItemTO();
            itemTO.setKey(item.getKey());
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setPurpose(item.getPurpose());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());

            if (itemTO.isConnObjectKey()) {
                containerTO.setConnObjectKeyItem(itemTO);
            } else {
                containerTO.add(itemTO);
            }

            itemTO.getTransformers().addAll(item.getTransformers().stream().
                    map(Entity::getKey).collect(Collectors.toSet()));
        });
    }

    @Override
    public ResourceTO getResourceTO(final ExternalResource resource) {
        ResourceTO resourceTO = new ResourceTO();

        // set the resource name
        resourceTO.setKey(resource.getKey());

        // set the connector instance
        ConnInstance connector = resource.getConnector();

        resourceTO.setConnector(Optional.ofNullable(connector).map(Entity::getKey).orElse(null));
        resourceTO.setConnectorDisplayName(Optional.ofNullable(connector)
                .map(ConnInstance::getDisplayName).orElse(null));

        // set the provision information
        resource.getProvisions().forEach(provision -> {
            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setKey(provision.getKey());
            provisionTO.setAnyType(provision.getAnyType().getKey());
            provisionTO.setObjectClass(provision.getObjectClass().getObjectClassValue());
            provisionTO.getAuxClasses().addAll(provision.getAuxClasses().stream().
                    map(Entity::getKey).collect(Collectors.toList()));
            provisionTO.setSyncToken(provision.getSerializedSyncToken());
            provisionTO.setIgnoreCaseMatch(provision.isIgnoreCaseMatch());
            if (provision.getUidOnCreate() != null) {
                provisionTO.setUidOnCreate(provision.getUidOnCreate().getKey());
            }

            if (provision.getMapping() != null) {
                MappingTO mappingTO = new MappingTO();
                provisionTO.setMapping(mappingTO);
                mappingTO.setConnObjectLink(provision.getMapping().getConnObjectLink());
                populateItems(provision.getMapping().getItems(), mappingTO);
            }

            virSchemaDAO.findByProvision(provision).forEach(virSchema -> {
                provisionTO.getVirSchemas().add(virSchema.getKey());

                MappingItem linkingMappingItem = virSchema.asLinkingMappingItem();

                ItemTO itemTO = new ItemTO();
                itemTO.setKey(linkingMappingItem.getKey());
                itemTO.setIntAttrName(linkingMappingItem.getIntAttrName());
                itemTO.setExtAttrName(linkingMappingItem.getExtAttrName());
                itemTO.setPurpose(linkingMappingItem.getPurpose());
                itemTO.setMandatoryCondition(linkingMappingItem.getMandatoryCondition());
                itemTO.setConnObjectKey(linkingMappingItem.isConnObjectKey());
                itemTO.setPassword(linkingMappingItem.isPassword());
                itemTO.setPropagationJEXLTransformer(linkingMappingItem.getPropagationJEXLTransformer());
                itemTO.setPullJEXLTransformer(linkingMappingItem.getPullJEXLTransformer());

                provisionTO.getMapping().getLinkingItems().add(itemTO);
            });

            resourceTO.getProvisions().add(provisionTO);
        });

        if (resource.getOrgUnit() != null) {
            OrgUnit orgUnit = resource.getOrgUnit();

            OrgUnitTO orgUnitTO = new OrgUnitTO();
            orgUnitTO.setKey(orgUnit.getKey());
            orgUnitTO.setObjectClass(orgUnit.getObjectClass().getObjectClassValue());
            orgUnitTO.setSyncToken(orgUnit.getSerializedSyncToken());
            orgUnitTO.setIgnoreCaseMatch(orgUnit.isIgnoreCaseMatch());
            orgUnitTO.setConnObjectLink(orgUnit.getConnObjectLink());
            populateItems(orgUnit.getItems(), orgUnitTO);

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

        resourceTO.setPropagationPolicy(resource.getPropagationPolicy() == null
                ? null : resource.getPropagationPolicy().getKey());

        resourceTO.setPullPolicy(resource.getPullPolicy() == null
                ? null : resource.getPullPolicy().getKey());

        resourceTO.setPushPolicy(resource.getPushPolicy() == null
                ? null : resource.getPushPolicy().getKey());

        resourceTO.setProvisionSorter(resource.getProvisionSorter() == null
                ? null : resource.getProvisionSorter().getKey());

        resourceTO.getConfOverride().addAll(resource.getConfOverride());
        Collections.sort(resourceTO.getConfOverride());

        resourceTO.setOverrideCapabilities(resource.isOverrideCapabilities());
        resourceTO.getCapabilitiesOverride().addAll(resource.getCapabilitiesOverride());

        resourceTO.getPropagationActions().addAll(
                resource.getPropagationActions().stream().map(Entity::getKey).collect(Collectors.toList()));

        return resourceTO;
    }
}
