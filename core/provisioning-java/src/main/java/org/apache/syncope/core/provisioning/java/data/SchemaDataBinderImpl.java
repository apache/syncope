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

import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaDataBinderImpl implements SchemaDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaDataBinder.class);

    private static final String[] IGNORE_PROPERTIES = { "anyTypeClass", "provision", "resource" };

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    // --------------- PLAIN -----------------
    private PlainSchema fill(final PlainSchema schema, final PlainSchemaTO schemaTO) {
        if (!JexlUtils.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            sce.getElements().add(schemaTO.getMandatoryCondition());
            throw sce;
        }

        BeanUtils.copyProperties(schemaTO, schema, IGNORE_PROPERTIES);

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
            hasAttrs |= plainSchemaDAO.findAttrs(schema, anyUtils.plainAttrClass()).isEmpty();
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
    public PlainSchemaTO getPlainSchemaTO(final PlainSchema schema) {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_PROPERTIES);
        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());

        return schemaTO;
    }

    // --------------- DERIVED -----------------
    private DerSchema fill(final DerSchema schema, final DerSchemaTO schemaTO) {
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

        BeanUtils.copyProperties(schemaTO, schema, IGNORE_PROPERTIES);

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
    public DerSchemaTO getDerSchemaTO(final DerSchema schema) {
        DerSchemaTO schemaTO = new DerSchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_PROPERTIES);
        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());

        return schemaTO;
    }

    // --------------- VIRTUAL -----------------
    private VirSchema fill(final VirSchema schema, final VirSchemaTO schemaTO) {
        BeanUtils.copyProperties(schemaTO, schema, IGNORE_PROPERTIES);

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
    public VirSchemaTO getVirSchemaTO(final VirSchema schema) {
        VirSchemaTO schemaTO = new VirSchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_PROPERTIES);
        schemaTO.setAnyTypeClass(schema.getAnyTypeClass() == null ? null : schema.getAnyTypeClass().getKey());
        schemaTO.setResource(schema.getProvision().getResource().getKey());
        schemaTO.setAnyType(schema.getProvision().getAnyType().getKey());

        return schemaTO;
    }
}
