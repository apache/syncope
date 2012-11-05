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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.to.SchemaTO;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;

@Component
public class SchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SchemaDataBinder.class);

    private static final String[] IGNORE_SCHEMA_PROPERTIES = { "derivedSchemas", "attributes" };

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractDerSchema> void populate(final AbstractSchema schema, final SchemaTO schemaTO)
            throws SyncopeClientCompositeErrorException {

        if (!jexlUtil.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidMandatoryCondition = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(schemaTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
            throw scce;
        }

        BeanUtils.copyProperties(schemaTO, schema, IGNORE_SCHEMA_PROPERTIES);
    }

    public void create(final SchemaTO schemaTO, final AbstractSchema schema)
            throws SyncopeClientCompositeErrorException {

        populate(schema, schemaTO);
    }

    public void update(final SchemaTO schemaTO, final AbstractSchema schema, final AttributableUtil attributableUtil)
            throws SyncopeClientCompositeErrorException {

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

    public <T extends AbstractSchema> SchemaTO getSchemaTO(final T schema, final AttributableUtil attributableUtil) {

        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_SCHEMA_PROPERTIES);

        return schemaTO;
    }
}
