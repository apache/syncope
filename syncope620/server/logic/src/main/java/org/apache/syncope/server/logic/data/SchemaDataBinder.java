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
package org.apache.syncope.server.logic.data;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.persistence.api.entity.AttributableUtil;
import org.apache.syncope.persistence.api.entity.DerSchema;
import org.apache.syncope.persistence.api.entity.PlainAttr;
import org.apache.syncope.persistence.api.entity.PlainSchema;
import org.apache.syncope.persistence.api.entity.VirSchema;
import org.apache.syncope.server.spring.BeanUtils;
import org.apache.syncope.server.utils.jexl.JexlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaDataBinder {

    @Autowired
    private PlainSchemaDAO schemaDAO;

    // --------------- PLAIN -----------------
    private <T extends PlainSchema> void fill(final T schema, final PlainSchemaTO schemaTO) {
        if (!JexlUtil.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            sce.getElements().add(schemaTO.getMandatoryCondition());
            throw sce;
        }

        BeanUtils.copyProperties(schemaTO, schema);
    }

    public <T extends PlainSchema> void create(final PlainSchemaTO schemaTO, final T schema) {
        fill(schema, schemaTO);
    }

    public <T extends PlainSchema> void update(final PlainSchemaTO schemaTO, final T schema,
            final AttributableUtil attributableUtil) {

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        List<PlainAttr> attrs = schemaDAO.findAttrs(schema, attributableUtil.plainAttrClass());
        if (!attrs.isEmpty()) {
            if (schema.getType() != schemaTO.getType()) {
                SyncopeClientException e = SyncopeClientException.build(
                        ClientExceptionType.valueOf("Invalid" + schema.getClass().getSimpleName()));
                e.getElements().add("Cannot change type since " + schema.getKey() + " has attributes");

                scce.addException(e);
            }
            if (schema.isUniqueConstraint() != schemaTO.isUniqueConstraint()) {
                SyncopeClientException e = SyncopeClientException.build(ClientExceptionType.valueOf("Invalid"
                        + schema.getClass().getSimpleName()));
                e.getElements().add("Cannot alter unique contraint since " + schema.getKey() + " has attributes");

                scce.addException(e);
            }
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        fill(schema, schemaTO);
    }

    public <T extends PlainSchema> PlainSchemaTO getSchemaTO(
            final T schema, final AttributableUtil attributableUtil) {

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
        } else if (!JexlUtil.isExpressionValid(derSchemaTO.getExpression())) {
            SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                    ClientExceptionType.InvalidValues);
            invalidMandatoryCondition.getElements().add(derSchemaTO.getExpression());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(derSchemaTO, derSchema);

        return derSchema;
    }

    public <T extends DerSchema> T create(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    public <T extends DerSchema> T update(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

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

    public <T extends VirSchema> T create(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    public <T extends VirSchema> T update(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    public <T extends VirSchema> VirSchemaTO getVirSchemaTO(final T virSchema) {
        VirSchemaTO virSchemaTO = new VirSchemaTO();
        BeanUtils.copyProperties(virSchema, virSchemaTO);

        return virSchemaTO;
    }
}
