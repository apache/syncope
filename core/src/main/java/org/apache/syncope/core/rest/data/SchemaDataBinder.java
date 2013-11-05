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
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
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

    private <T extends AbstractDerSchema> void populate(final AbstractSchema schema, final SchemaTO schemaTO) {
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

    public void create(final SchemaTO schemaTO, final AbstractSchema schema) {
        populate(schema, schemaTO);
    }

    public void update(final SchemaTO schemaTO, final AbstractSchema schema, final AttributableUtil attributableUtil) {
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

        populate(schema, schemaTO);
    }

    public <T extends AbstractSchema> SchemaTO getSchemaTO(final T schema) {
        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO);

        return schemaTO;
    }
}
