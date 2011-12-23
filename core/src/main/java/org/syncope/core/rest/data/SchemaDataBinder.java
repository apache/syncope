/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.util.AttributableUtil;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class SchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SchemaDataBinder.class);

    private static final String[] IGNORE_SCHEMA_PROPERTIES = {
        "derivedSchemas", "attributes"};

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractDerSchema> void populate(
            final AbstractSchema schema,
            final SchemaTO schemaTO)
            throws SyncopeClientCompositeErrorException {

        if (!jexlUtil.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidMandatoryCondition =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(
                    schemaTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
            throw scce;
        }

        BeanUtils.copyProperties(schemaTO, schema, IGNORE_SCHEMA_PROPERTIES);
    }

    public void create(final SchemaTO schemaTO, final AbstractSchema schema)
            throws SyncopeClientCompositeErrorException {

        populate(schema, schemaTO);
    }

    public void update(final SchemaTO schemaTO, final AbstractSchema schema,
            final AttributableUtil attributableUtil)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        List<AbstractAttr> attrs = schemaDAO.getAttributes(
                schema, attributableUtil.attributeClass());
        if (!attrs.isEmpty() && schema.getType() != schemaTO.getType()) {
            SyncopeClientException e = new SyncopeClientException(
                    SyncopeClientExceptionType.valueOf(
                    "Invalid" + schema.getClass().getSimpleName()));
            e.addElement("Cannot change type since " + schema.getName()
                    + " has attributes");

            scce.addException(e);
            throw scce;
        }

        populate(schema, schemaTO);
    }

    public <T extends AbstractSchema> SchemaTO getSchemaTO(final T schema,
            final AttributableUtil attributableUtil) {

        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_SCHEMA_PROPERTIES);

        return schemaTO;
    }
}
