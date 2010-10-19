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
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
@Transactional(rollbackFor = {
    Throwable.class
})
public class SchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SchemaDataBinder.class);
    private static final String[] ignoreSchemaProperties = {
        "derivedSchemas", "attributes"};
    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;
    @Autowired
    private JexlEngine jexlEngine;

    private <T extends AbstractDerivedSchema> AbstractSchema populate(
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

        try {
            jexlEngine.createExpression(schemaTO.getMandatoryCondition());
        } catch (JexlException e) {
            LOG.error("Invalid mandatory condition: "
                    + schemaTO.getMandatoryCondition(), e);

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

        BeanUtils.copyProperties(schemaTO, schema, ignoreSchemaProperties);

        AbstractDerivedSchema abstractDerivedSchema;
        for (String derivedSchema : schemaTO.getDerivedSchemas()) {
            abstractDerivedSchema =
                    derivedSchemaDAO.find(derivedSchema, derivedReference);
            if (abstractDerivedSchema != null) {
                schema.addDerivedSchema(abstractDerivedSchema);
            } else {
                LOG.error("Unmatched derived schema name: " + derivedSchema);
            }
        }

        return schema;
    }

    public <T extends AbstractDerivedSchema> AbstractSchema create(
            final SchemaTO schemaTO,
            AbstractSchema schema,
            final Class<T> derivedReference)
            throws SyncopeClientCompositeErrorException {

        return populate(schema, schemaTO, derivedReference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <T extends AbstractDerivedSchema> AbstractSchema update(
            final SchemaTO schemaTO,
            AbstractSchema schema,
            final Class<T> derivedReference)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        schema = populate(schema, schemaTO, derivedReference, scce);

        boolean validationExceptionFound = false;
        AbstractAttribute attribute;
        AbstractAttributeValue attributeValue;
        for (Iterator<? extends AbstractAttribute> aItor =
                schema.getAttributes().iterator();
                aItor.hasNext() && !validationExceptionFound;) {

            attribute = aItor.next();
            for (Iterator<? extends AbstractAttributeValue> avItor =
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
                    SyncopeClientExceptionType.InvalidUpdate);
            e.addElement(schema.getName());

            scce.addException(e);
            throw scce;
        }

        return schema;
    }

    public <T extends AbstractSchema> SchemaTO getSchemaTO(T schema) {
        SchemaTO schemaTO = new SchemaTO();
        BeanUtils.copyProperties(schema, schemaTO, ignoreSchemaProperties);

        for (AbstractDerivedSchema derivedSchema : schema.getDerivedSchemas()) {
            schemaTO.addDerivedSchema(derivedSchema.getName());
        }
        schemaTO.setAttributes(schema.getAttributes().size());

        return schemaTO;
    }
}
