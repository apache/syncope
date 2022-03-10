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

import java.util.stream.Collectors;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.SchemaLabel;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
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

    protected <S extends Schema, T extends SchemaTO> void labels(final T src, final S dst) {
        src.getLabels().forEach((locale, display) -> {
            SchemaLabel label = dst.getLabel(locale).orElse(null);
            if (label == null) {
                label = entityFactory.newEntity(SchemaLabel.class);
                label.setLocale(locale);
                label.setSchema(dst);
                dst.add(label);
            }
            label.setDisplay(display);
        });

        dst.getLabels().removeIf(label -> !src.getLabels().containsKey(label.getLocale()));
    }

    protected static <S extends Schema, T extends SchemaTO> void labels(final S src, final T dst) {
        dst.getLabels().putAll(src.getLabels().stream().
                collect(Collectors.toMap(SchemaLabel::getLocale, SchemaLabel::getDisplay)));
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
        schema.setEnumerationKeys(schemaTO.getEnumerationKeys());
        schema.setEnumerationValues(schemaTO.getEnumerationValues());
        schema.setMandatoryCondition(schemaTO.getMandatoryCondition());
        schema.setMimeType(schemaTO.getMimeType());
        schema.setMultivalue(schemaTO.isMultivalue());
        schema.setReadonly(schemaTO.isReadonly());
        schema.setSecretKey(schemaTO.getSecretKey());
        schema.setUniqueConstraint(schemaTO.isUniqueConstraint());

        labels(schemaTO, schema);

        if (schemaTO.getValidator() == null) {
            schema.setValidator(null);
        } else {
            Implementation validator = implementationDAO.find(schemaTO.getValidator());
            if (validator == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...",
                        schemaTO.getValidator());
            } else {
                schema.setValidator(validator);
            }
        }

        PlainSchema merged = plainSchemaDAO.save(schema);

        if (schemaTO.getAnyTypeClass() != null
                && (merged.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(merged.getAnyTypeClass().getKey()))) {

            AnyTypeClass anyTypeClass = anyTypeClassDAO.find(schemaTO.getAnyTypeClass());
            if (anyTypeClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName()
                        + "{}, ignoring...", schemaTO.getAnyTypeClass());
            } else {
                anyTypeClass.add(merged);
                merged.setAnyTypeClass(anyTypeClass);
            }
        } else if (schemaTO.getAnyTypeClass() == null && merged.getAnyTypeClass() != null) {
            merged.getAnyTypeClass().getPlainSchemas().remove(merged);
            merged.setAnyTypeClass(null);
        }

        return merged;
    }

    @Override
    public PlainSchema create(final PlainSchemaTO schemaTO) {
        return fill(entityFactory.newEntity(PlainSchema.class), schemaTO);
    }

    @Override
    public PlainSchema update(final PlainSchemaTO schemaTO, final PlainSchema schema) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        boolean hasAttrs = false;
        for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
            AnyUtils anyUtils = anyUtilsFactory.getInstance(anyTypeKind);
            hasAttrs |= plainSchemaDAO.hasAttrs(schema, anyUtils.plainAttrClass());
        }

        if (hasAttrs) {
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
        PlainSchema schema = plainSchemaDAO.find(key);
        if (schema == null) {
            throw new NotFoundException("Schema '" + key + '\'');
        }

        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setType(schema.getType());
        schemaTO.setCipherAlgorithm(schema.getCipherAlgorithm());
        schemaTO.setConversionPattern(schema.getConversionPattern());
        schemaTO.setEnumerationKeys(schema.getEnumerationKeys());
        schemaTO.setEnumerationValues(schema.getEnumerationValues());
        schemaTO.setMandatoryCondition(schema.getMandatoryCondition());
        schemaTO.setMimeType(schema.getMimeType());
        schemaTO.setMultivalue(schema.isMultivalue());
        schemaTO.setReadonly(schema.isReadonly());
        schemaTO.setSecretKey(schema.getSecretKey());
        schemaTO.setUniqueConstraint(schema.isUniqueConstraint());

        labels(schema, schemaTO);

        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());
        if (schema.getValidator() != null) {
            schemaTO.setValidator(schema.getValidator().getKey());
        }

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

        labels(schemaTO, schema);

        DerSchema merged = derSchemaDAO.save(schema);

        if (schemaTO.getAnyTypeClass() != null
                && (merged.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(merged.getAnyTypeClass().getKey()))) {

            AnyTypeClass anyTypeClass = anyTypeClassDAO.find(schemaTO.getAnyTypeClass());
            if (anyTypeClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName()
                        + "{}, ignoring...", schemaTO.getAnyTypeClass());
            } else {
                anyTypeClass.add(merged);
                merged.setAnyTypeClass(anyTypeClass);
            }
        } else if (schemaTO.getAnyTypeClass() == null && merged.getAnyTypeClass() != null) {
            merged.getAnyTypeClass().getDerSchemas().remove(merged);
            merged.setAnyTypeClass(null);
        }

        return merged;
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
        DerSchema schema = derSchemaDAO.find(key);
        if (schema == null) {
            throw new NotFoundException("Derived schema '" + key + '\'');
        }

        DerSchemaTO schemaTO = new DerSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setExpression(schema.getExpression());

        labels(schema, schemaTO);

        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());

        return schemaTO;
    }

    // --------------- VIRTUAL -----------------
    protected VirSchema fill(final VirSchema schema, final VirSchemaTO schemaTO) {
        schema.setKey(schemaTO.getKey());
        schema.setExtAttrName(schemaTO.getExtAttrName());
        schema.setReadonly(schemaTO.isReadonly());

        labels(schemaTO, schema);

        if (schemaTO.getAnyTypeClass() != null
                && (schema.getAnyTypeClass() == null
                || !schemaTO.getAnyTypeClass().equals(schema.getAnyTypeClass().getKey()))) {

            AnyTypeClass anyTypeClass = anyTypeClassDAO.find(schemaTO.getAnyTypeClass());
            if (anyTypeClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName()
                        + "{}, ignoring...", schemaTO.getAnyTypeClass());
            } else {
                anyTypeClass.add(schema);
                schema.setAnyTypeClass(anyTypeClass);
            }
        } else if (schemaTO.getAnyTypeClass() == null && schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().getVirSchemas().remove(schema);
            schema.setAnyTypeClass(null);
        }

        ExternalResource resource = resourceDAO.find(schemaTO.getResource());
        if (resource == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("Resource " + schemaTO.getResource() + " not found");
            throw sce;
        }
        AnyType anyType = anyTypeDAO.find(schemaTO.getAnyType());
        if (anyType == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("AnyType " + schemaTO.getAnyType() + " not found");
            throw sce;
        }
        Provision provision = resource.getProvision(anyType).orElse(null);
        if (provision == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSchemaDefinition);
            sce.getElements().add("Provision for AnyType" + schemaTO.getAnyType()
                    + " not found in " + schemaTO.getResource());
            throw sce;
        }
        schema.setProvision(provision);

        return virSchemaDAO.save(schema);
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
        VirSchema schema = virSchemaDAO.find(key);
        if (schema == null) {
            throw new NotFoundException("Virtual Schema '" + key + '\'');
        }

        VirSchemaTO schemaTO = new VirSchemaTO();
        schemaTO.setKey(schema.getKey());
        schemaTO.setExtAttrName(schema.getExtAttrName());
        schemaTO.setReadonly(schema.isReadonly());

        labels(schema, schemaTO);

        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());
        schemaTO.setResource(schema.getProvision().getResource().getKey());
        schemaTO.setAnyType(schema.getProvision().getAnyType().getKey());

        return schemaTO;
    }
}
