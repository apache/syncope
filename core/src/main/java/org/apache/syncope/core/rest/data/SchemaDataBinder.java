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
package org.apache.syncope.core.rest.data;

import java.util.List;
import org.apache.syncope.common.to.DerSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.VirSchemaTO;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.JexlUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SchemaDataBinder {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private JexlUtil jexlUtil;

    // --------------- NORMAL -----------------
    private <T extends AbstractSchema> void fill(final T schema, final SchemaTO schemaTO) {
        if (!jexlUtil.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(schemaTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
            throw scce;
        }

        BeanUtils.copyProperties(schemaTO, schema);
    }

    public <T extends AbstractSchema> void create(final SchemaTO schemaTO, final T schema) {
        fill(schema, schemaTO);
    }

    public <T extends AbstractSchema> void update(final SchemaTO schemaTO, final T schema,
            final AttributableUtil attributableUtil) {

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        List<AbstractAttr> attrs = schemaDAO.getAttributes(schema, attributableUtil.attrClass());
        if (!attrs.isEmpty()) {
            if (schema.getType() != schemaTO.getType()) {
                SyncopeClientException e = new SyncopeClientException(SyncopeClientExceptionType.valueOf("Invalid"
                        + schema.getClass().getSimpleName()));
                e.addElement("Cannot change type since " + schema.getName() + " has attributes");

                scce.addException(e);
            }
            if (schema.isUniqueConstraint() != schemaTO.isUniqueConstraint()) {
                SyncopeClientException e = new SyncopeClientException(SyncopeClientExceptionType.valueOf("Invalid"
                        + schema.getClass().getSimpleName()));
                e.addElement("Cannot alter unique contraint since " + schema.getName() + " has attributes");

                scce.addException(e);
            }
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        fill(schema, schemaTO);
    }

    public <T extends AbstractSchema> SchemaTO getSchemaTO(final T schema, final AttributableUtil attributableUtil) {
        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO);

        return schemaTO;
    }

    // --------------- DERIVED -----------------
    private <T extends AbstractDerSchema> T populate(final T derSchema, final DerSchemaTO derSchemaTO) {
        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        if (derSchemaTO.getExpression() == null) {
            SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.addElement("expression");

            scce.addException(requiredValuesMissing);
        }

        if (!jexlUtil.isExpressionValid(derSchemaTO.getExpression())) {
            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(derSchemaTO.getExpression());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(derSchemaTO, derSchema);

        return derSchema;
    }

    public <T extends AbstractDerSchema> T create(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    public <T extends AbstractDerSchema> T update(final DerSchemaTO derSchemaTO, final T derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    public <T extends AbstractDerSchema> DerSchemaTO getDerSchemaTO(final T derSchema) {
        DerSchemaTO derSchemaTO = new DerSchemaTO();
        BeanUtils.copyProperties(derSchema, derSchemaTO);

        return derSchemaTO;
    }

    // --------------- VIRTUAL -----------------
    private <T extends AbstractVirSchema> T fill(final T virSchema, final VirSchemaTO virSchemaTO) {
        BeanUtils.copyProperties(virSchemaTO, virSchema);

        return virSchema;
    }

    public <T extends AbstractVirSchema> T create(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    public <T extends AbstractVirSchema> T update(final VirSchemaTO virSchemaTO, final T virSchema) {
        return fill(virSchema, virSchemaTO);
    }

    public <T extends AbstractVirSchema> VirSchemaTO getVirSchemaTO(final T virSchema) {
        VirSchemaTO virtualSchemaTO = new VirSchemaTO();
        BeanUtils.copyProperties(virSchema, virtualSchemaTO);

        return virtualSchemaTO;
    }
}
