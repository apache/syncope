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

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaDataBinderImpl implements SchemaDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(SchemaDataBinder.class);

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ImplementationDAO implementationDAO;

    protected final EntityFactory entityFactory;

    protected final AnyUtilsFactory anyUtilsFactory;

    public SchemaDataBinderImpl(
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory) {

        this.anyTypeClassDAO = anyTypeClassDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.resourceDAO = resourceDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.implementationDAO = implementationDAO;
        this.entityFactory = entityFactory;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    // --------------- PLAIN -----------------
    protected PlainSchema fill(final PlainSchema schema, final PlainSchemaTO schemaTO) {
        if (!JexlUtils.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            sce.getElements().add(schemaTO.getMandatoryCondition());
            throw sce;
        }

        schema.setKey(schemaTO.getKey());
        schema.setType(schemaTO.getType());
        schema.setCipherAlgorithm(schemaTO.getCipherAlgorithm());
        schema.setConversionPattern(schemaTO.getConversionPattern());
        schema.getEnumValues().clear();
        schema.getEnumValues().putAll(schemaTO.getEnumValues());

        if (schemaTO.getDropdownValueProvider() == null) {
            schema.setDropdownValueProvider(null);
        } else {
            implementationDAO.findById(schemaTO.getDropdownValueProvider()).ifPresentOrElse(
                    schema::setDropdownValueProvider,
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            Implementation.class.getSimpleName(), schemaTO.getDropdownValueProvider()));
        }

        schema.setMandatoryCondition(schemaTO.getMandatoryCondition());
        schema.setMimeType(schemaTO.getMimeType());
        schema.setMultivalue(schemaTO.isMultivalue());
        schema.setReadonly(schemaTO.isReadonly());
        schema.setSecretKey(schemaTO.getSecretKey());
        schema.setUniqueConstraint(schemaTO.isUniqueConstraint());

        schema.getLabels().clear();
        schema.getLabels().putAll(schemaTO.getLabels());

        if (schemaTO.getValidator() == null) {
            schema.setValidator(null);
        } else {
            implementationDAO.findById(schemaTO.getValidator()).ifPresentOrElse(
                    schema::setValidator,
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            Implementation.class.getSimpleName(), schemaTO.getValidator()));
        }

        PlainSchema saved = plainSchemaDAO.save(schema);

        Mutable<AnyTypeClass> atc = new MutableObject<>();
        if (schemaTO.getAnyTypeClass() != null
                && (saved.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(saved.getAnyTypeClass().getKey()))) {

            anyTypeClassDAO.findById(schemaTO.getAnyTypeClass()).ifPresentOrElse(
                    anyTypeClass -> {
                        anyTypeClass.add(saved);
                        saved.setAnyTypeClass(anyTypeClass);

                        atc.setValue(anyTypeClass);
                    },
                    () -> LOG.debug("Invalid {}{}, ignoring...",
                            AnyTypeClass.class.getSimpleName(), schemaTO.getAnyTypeClass()));
        } else if (schemaTO.getAnyTypeClass() == null && saved.getAnyTypeClass() != null) {
            saved.getAnyTypeClass().getPlainSchemas().remove(saved);
            saved.setAnyTypeClass(null);

            atc.setValue(saved.getAnyTypeClass());
        }

        PlainSchema filled = plainSchemaDAO.save(saved);
        Optional.ofNullable(atc.getValue()).ifPresent(anyTypeClassDAO::save);
        return filled;
    }

    @Override
    public PlainSchema create(final PlainSchemaTO schemaTO) {
        return fill(entityFactory.newEntity(PlainSchema.class), schemaTO);
    }

    @Override
    public PlainSchema update(final PlainSchemaTO schemaTO, final PlainSchema schema) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        if (plainSchemaDAO.hasAttrs(schema)) {
            if (schema.getType() != schemaTO.getType()) {
                SyncopeClientException e = SyncopeClientException.build(ClientExceptionType.InvalidPlainSchema);
                e.getElements().add("Cannot change type since " + schema.getKey() + " has attributes");

                scce.addException(e);
            }
            if (schema.isUniqueConstraint() != schemaTO.isUniqueConstraint()) {
                SyncopeClientException e = SyncopeClientException.build(ClientExceptionType.InvalidPlainSchema);
                e.getElements().add("Cannot alter unique contraint since " + schema.getKey() + " has attributes");

                scce.addException(e);
            }
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        return fill(schema, schemaTO);
    }

    @Override
    public PlainSchemaTO getPlainSchemaTO(final String key) {
        PlainSchema schema = plainSchemaDAO.findById(key).
                orElseThrow(() -> new NotFoundException("PlainSchema " + key));

        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setType(schema.getType());
        schemaTO.setCipherAlgorithm(schema.getCipherAlgorithm());
        schemaTO.setConversionPattern(schema.getConversionPattern());
        schemaTO.getEnumValues().putAll(schema.getEnumValues());
        Optional.ofNullable(schema.getDropdownValueProvider()).
                ifPresent(v -> schemaTO.setDropdownValueProvider(v.getKey()));
        schemaTO.setMandatoryCondition(schema.getMandatoryCondition());
        schemaTO.setMimeType(schema.getMimeType());
        schemaTO.setMultivalue(schema.isMultivalue());
        schemaTO.setReadonly(schema.isReadonly());
        schemaTO.setSecretKey(schema.getSecretKey());
        schemaTO.setUniqueConstraint(schema.isUniqueConstraint());
        schemaTO.getLabels().putAll(schema.getLabels());
        Optional.ofNullable(schema.getAnyTypeClass()).
                ifPresent(v -> schemaTO.setAnyTypeClass(v.getKey()));
        Optional.ofNullable(schema.getValidator()).
                ifPresent(v -> schemaTO.setValidator(v.getKey()));

        return schemaTO;
    }

    // --------------- DERIVED -----------------
    protected DerSchema fill(final DerSchema schema, final DerSchemaTO schemaTO) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        if (StringUtils.isBlank(schemaTO.getExpression())) {
            SyncopeClientException requiredValuesMissing =
                    SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.getElements().add("expression");

            scce.addException(requiredValuesMissing);
        } else if (!JexlUtils.isExpressionValid(schemaTO.getExpression())) {
            SyncopeClientException e = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            e.getElements().add(schemaTO.getExpression());

            scce.addException(e);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        schema.setKey(schemaTO.getKey());
        schema.setExpression(schemaTO.getExpression());

        schema.getLabels().clear();
        schema.getLabels().putAll(schemaTO.getLabels());

        DerSchema saved = derSchemaDAO.save(schema);

        Mutable<AnyTypeClass> atc = new MutableObject<>();
        if (schemaTO.getAnyTypeClass() != null
                && (saved.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(saved.getAnyTypeClass().getKey()))) {

            anyTypeClassDAO.findById(schemaTO.getAnyTypeClass()).ifPresentOrElse(
                    anyTypeClass -> {
                        anyTypeClass.add(saved);
                        saved.setAnyTypeClass(anyTypeClass);

                        atc.setValue(anyTypeClass);
                    },
                    () -> LOG.debug("Invalid {}{}, ignoring...",
                            AnyTypeClass.class.getSimpleName(), schemaTO.getAnyTypeClass()));
        } else if (schemaTO.getAnyTypeClass() == null && saved.getAnyTypeClass() != null) {
            saved.getAnyTypeClass().getDerSchemas().remove(saved);
            saved.setAnyTypeClass(null);

            atc.setValue(saved.getAnyTypeClass());
        }

        DerSchema filled = derSchemaDAO.save(saved);
        Optional.ofNullable(atc.getValue()).ifPresent(anyTypeClassDAO::save);
        return filled;
    }

    @Override
    public DerSchema create(final DerSchemaTO schemaTO) {
        return fill(entityFactory.newEntity(DerSchema.class), schemaTO);
    }

    @Override
    public DerSchema update(final DerSchemaTO schemaTO, final DerSchema schema) {
        return fill(schema, schemaTO);
    }

    @Override
    public DerSchemaTO getDerSchemaTO(final String key) {
        DerSchema schema = derSchemaDAO.findById(key).
                orElseThrow(() -> new NotFoundException("DerSchema " + key));

        DerSchemaTO schemaTO = new DerSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setExpression(schema.getExpression());
        schemaTO.getLabels().putAll(schema.getLabels());
        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());
        return schemaTO;
    }

    // --------------- VIRTUAL -----------------
    protected VirSchema fill(final VirSchema schema, final VirSchemaTO schemaTO) {
        schema.setKey(schemaTO.getKey());
        schema.setExtAttrName(schemaTO.getExtAttrName());
        schema.setReadonly(schemaTO.isReadonly());

        schema.getLabels().clear();
        schema.getLabels().putAll(schemaTO.getLabels());

        ExternalResource resource = resourceDAO.findById(schemaTO.getResource()).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(
                    ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("Resource " + schemaTO.getResource() + " not found");
            return sce;
        });
        AnyType anyType = anyTypeDAO.findById(schemaTO.getAnyType()).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("AnyType " + schemaTO.getAnyType() + " not found");
            return sce;
        });
        resource.getProvisionByAnyType(anyType.getKey()).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("Provision for AnyType" + schemaTO.getAnyType()
                    + " not found in " + schemaTO.getResource());
            return sce;
        });
        schema.setResource(resource);
        schema.setAnyType(anyType);

        VirSchema saved = virSchemaDAO.save(schema);

        Mutable<AnyTypeClass> atc = new MutableObject<>();
        if (schemaTO.getAnyTypeClass() != null
                && (saved.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(saved.getAnyTypeClass().getKey()))) {

            anyTypeClassDAO.findById(schemaTO.getAnyTypeClass()).ifPresentOrElse(
                    anyTypeClass -> {
                        anyTypeClass.add(saved);
                        saved.setAnyTypeClass(anyTypeClass);

                        atc.setValue(anyTypeClass);
                    },
                    () -> LOG.debug("Invalid {}{}, ignoring...",
                            AnyTypeClass.class.getSimpleName(), schemaTO.getAnyTypeClass()));
        } else if (schemaTO.getAnyTypeClass() == null && saved.getAnyTypeClass() != null) {
            saved.getAnyTypeClass().getVirSchemas().remove(saved);
            saved.setAnyTypeClass(null);

            atc.setValue(saved.getAnyTypeClass());
        }

        VirSchema filled = virSchemaDAO.save(saved);
        Optional.ofNullable(atc.getValue()).ifPresent(anyTypeClassDAO::save);
        return filled;
    }

    @Override
    public VirSchema create(final VirSchemaTO schemaTO) {
        return fill(entityFactory.newEntity(VirSchema.class), schemaTO);
    }

    @Override
    public VirSchema update(final VirSchemaTO schemaTO, final VirSchema schema) {
        return fill(schema, schemaTO);
    }

    @Override
    public VirSchemaTO getVirSchemaTO(final String key) {
        VirSchema schema = virSchemaDAO.findById(key).
                orElseThrow(() -> new NotFoundException("VirSchema " + key));

        VirSchemaTO schemaTO = new VirSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setExtAttrName(schema.getExtAttrName());
        schemaTO.setReadonly(schema.isReadonly());
        schemaTO.getLabels().putAll(schema.getLabels());
        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());
        schemaTO.setResource(schema.getResource().getKey());
        schemaTO.setAnyType(schema.getAnyType().getKey());
        return schemaTO;
    }
}
