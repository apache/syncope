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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.to.DerivedSchemaTO;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;

@Component
public class DerivedSchemaDataBinder {

    private static final String[] ignoreDerivedSchemaProperties = { "schemas", "derivedAttributes" };

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractSchema> AbstractDerSchema populate(final AbstractDerSchema derivedSchema,
            final DerivedSchemaTO derivedSchemaTO, final SyncopeClientCompositeErrorException scce)
            throws SyncopeClientCompositeErrorException {

        if (derivedSchemaTO.getExpression() == null) {
            SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.addElement("expression");

            scce.addException(requiredValuesMissing);
        }

        if (!jexlUtil.isExpressionValid(derivedSchemaTO.getExpression())) {
            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(derivedSchemaTO.getExpression());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(derivedSchemaTO, derivedSchema, ignoreDerivedSchemaProperties);

        return derivedSchema;
    }

    public <T extends AbstractSchema> AbstractDerSchema create(final DerivedSchemaTO derivedSchemaTO,
            final AbstractDerSchema derivedSchema) {

        return populate(derivedSchema, derivedSchemaTO,
                new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST));
    }

    public AbstractDerSchema update(final DerivedSchemaTO derivedSchemaTO, final AbstractDerSchema derivedSchema) {

        return populate(derivedSchema, derivedSchemaTO,
                new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST));
    }

    public <T extends AbstractDerSchema> DerivedSchemaTO getDerivedSchemaTO(final T derivedSchema) {

        DerivedSchemaTO derivedSchemaTO = new DerivedSchemaTO();
        BeanUtils.copyProperties(derivedSchema, derivedSchemaTO, ignoreDerivedSchemaProperties);

        return derivedSchemaTO;
    }
}
