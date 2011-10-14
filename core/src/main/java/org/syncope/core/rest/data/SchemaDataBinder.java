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

import java.util.Iterator;
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
import org.syncope.core.persistence.beans.AbstractAttrValue;
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

    private <T extends AbstractDerSchema> AbstractSchema populate(
            final AbstractSchema schema,
            final SchemaTO schemaTO,
            final Class<T> derivedReference,
            final SyncopeClientCompositeErrorException scce)
            throws SyncopeClientCompositeErrorException {

        if (schemaTO.getMandatoryCondition() == null) {
            SyncopeClientException requiredValuesMissing =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.addElement("mandatoryCondition");

            scce.addException(requiredValuesMissing);
        }

        if (!jexlUtil.isExpressionValid(schemaTO.getMandatoryCondition())) {
            SyncopeClientException invalidMandatoryCondition =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(
                    schemaTO.getMandatoryCondition());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(schemaTO, schema, IGNORE_SCHEMA_PROPERTIES);

        return schema;
    }

    public <T extends AbstractDerSchema> AbstractSchema create(
            final SchemaTO schemaTO,
            AbstractSchema schema,
            final Class<T> derivedReference)
            throws SyncopeClientCompositeErrorException {

        return populate(schema, schemaTO, derivedReference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <T extends AbstractDerSchema> AbstractSchema update(
            final SchemaTO schemaTO,
            AbstractSchema schema,
            final AttributableUtil attributableUtil)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        schema = populate(schema, schemaTO,
                attributableUtil.derivedSchemaClass(), scce);

        boolean validationExceptionFound = false;
        AbstractAttr attribute;
        AbstractAttrValue attributeValue;
        for (Iterator<? extends AbstractAttr> aItor = schemaDAO.getAttributes(
                schema, attributableUtil.attributeClass()).iterator();
                aItor.hasNext() && !validationExceptionFound;) {

            attribute = aItor.next();
            for (Iterator<? extends AbstractAttrValue> avItor =
                    attribute.getValues().iterator();
                    avItor.hasNext() && !validationExceptionFound;) {

                attributeValue = avItor.next();
                try {
                    schema.getValidator().getValue(
                            attributeValue.getValueAsString(),
                            attributeValue);
                } catch (Exception e) {
                    validationExceptionFound = true;
                }
            }
        }

        if (validationExceptionFound) {
            SyncopeClientException e = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidSchema);
            e.addElement(schema.getName());

            scce.addException(e);
            throw scce;
        }

        return schema;
    }

    public <T extends AbstractSchema> SchemaTO getSchemaTO(T schema,
            final AttributableUtil attributableUtil) {

        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, IGNORE_SCHEMA_PROPERTIES);

        return schemaTO;
    }
}
