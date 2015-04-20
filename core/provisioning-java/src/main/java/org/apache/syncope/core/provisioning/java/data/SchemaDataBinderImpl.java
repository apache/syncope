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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaDataBinderImpl implements SchemaDataBinder {

    @Autowired
    private PlainSchemaDAO schemaDAO;

    // --------------- PLAIN -----------------
    private <T extends PlainSchema> void fill(final T schema, final PlainSchemaTO schemaTO) {
        if (!JexlUtils.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            sce.getElements().add(schemaTO.getMandatoryCondition());
            throw sce;
        }

        BeanUtils.copyProperties(schemaTO, schema);
    }

    @Override
    public <T extends PlainSchema> void create(final PlainSchemaTO schemaTO, final T schema) {
        fill(schema, schemaTO);
    }

    @Override
    public <T extends PlainSchema> void update(final PlainSchemaTO schemaTO, final T schema,
            final AttributableUtils attributableUtil) {

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        List<PlainAttr> attrs = schemaDAO.findAttrs(schema, attributableUtil.plainAttrClass());
        if (!attrs.isEmpty()) {
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

        fill(schema, schemaTO);
    }

    @Override
    public <T extends PlainSchema> PlainSchemaTO getPlainSchemaTO(
            final T schema, final AttributableUtils attributableUtil) {

        PlainSchemaTO schemaTO = new PlainSchemaTO();
        BeanUtils.copyProperties(schema, schemaTO);

        return schemaTO;
    }

    // --------------- DERIVED -----------------
    private <T extends DerSchema> T populate(final T derSchema, final DerSchemaTO derSchemaTO) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        if (StringUtils.isBlank(derSchemaTO.getExpression())) {
            SyncopeClientException requiredValuesMissing =
                    SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.getElements().add("expression");

            scce.addException(requiredValuesMissing);
        } else if (!JexlUtils.isExpressionValid(derSchemaTO.getExpression())) {
            SyncopeClientException e = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            e.getElements().add(derSchemaTO.getExpression());

            scce.addException(e);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(derSchemaTO, derSchema);

        return derSchema;
    }

    @Override
    public <T extends DerSchema> T create(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    @Override
    public <T extends DerSchema> T update(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    @Override
    public <T extends DerSchema> DerSchemaTO getDerSchemaTO(final T derSchema) {
        DerSchemaTO derSchemaTO = new DerSchemaTO();
        BeanUtils.copyProperties(derSchema, derSchemaTO);

        return derSchemaTO;
    }

    // --------------- VIRTUAL -----------------
    private <T extends VirSchema> T fill(final T virSchema, final VirSchemaTO virSchemaTO) {
        BeanUtils.copyProperties(virSchemaTO, virSchema);

        return virSchema;
    }

    @Override
    public <T extends VirSchema> T create(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    @Override
    public <T extends VirSchema> T update(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    @Override
    public <T extends VirSchema> VirSchemaTO getVirSchemaTO(final T virSchema) {
        VirSchemaTO virSchemaTO = new VirSchemaTO();
        BeanUtils.copyProperties(virSchema, virSchemaTO);

        return virSchemaTO;
    }
}
