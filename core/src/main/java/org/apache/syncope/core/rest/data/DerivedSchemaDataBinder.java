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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.DerivedSchemaTO;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.util.JexlUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DerivedSchemaDataBinder {

    @Autowired
    private JexlUtil jexlUtil;

    private AbstractDerSchema populate(final AbstractDerSchema derSchema, final DerivedSchemaTO derSchemaTO) {
        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        if (StringUtils.isBlank(derSchemaTO.getExpression())) {
            SyncopeClientException requiredValuesMissing =
                    new SyncopeClientException(SyncopeClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.addElement("expression");

            scce.addException(requiredValuesMissing);
        } else if (!jexlUtil.isExpressionValid(derSchemaTO.getExpression())) {
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

    public AbstractDerSchema create(final DerivedSchemaTO derSchemaTO, final AbstractDerSchema derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    public AbstractDerSchema update(final DerivedSchemaTO derSchemaTO, final AbstractDerSchema derSchema) {
        return populate(derSchema, derSchemaTO);
    }

    public <T extends AbstractDerSchema> DerivedSchemaTO getDerivedSchemaTO(final T derSchema) {
        DerivedSchemaTO derSchemaTO = new DerivedSchemaTO();
        BeanUtils.copyProperties(derSchema, derSchemaTO);

        return derSchemaTO;
    }
}
