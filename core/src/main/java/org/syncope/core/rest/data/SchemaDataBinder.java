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
@Transactional(rollbackFor = {Throwable.class})
public class SchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SchemaDataBinder.class);
    private static final String[] ignoreSchemaProperties = {
        "derivedSchemas", "attributes"};
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;

    private <T extends AbstractSchema, K extends AbstractDerivedSchema> T populateSchema(
            T schema,
            SchemaTO schemaTO,
            Class<K> derivedReference) {

        BeanUtils.copyProperties(schemaTO, schema, ignoreSchemaProperties);

        AbstractDerivedSchema abstractDerivedSchema = null;
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

    public <T extends AbstractSchema, K extends AbstractDerivedSchema> T createSchema(
            SchemaTO schemaTO,
            T schema,
            Class<K> derivedReference) {

        return populateSchema(schema, schemaTO, derivedReference);
    }

    public <T extends AbstractSchema, K extends AbstractDerivedSchema> T updateSchema(
            SchemaTO schemaTO,
            Class<T> reference,
            Class<K> derivedReference)
            throws SyncopeClientCompositeErrorException {

        T schema = schemaDAO.find(schemaTO.getName(), reference);
        if (schema != null) {
            schema = populateSchema(schema, schemaTO, derivedReference);

            boolean validationExceptionFound = false;
            AbstractAttribute attribute = null;
            AbstractAttributeValue attributeValue = null;
            for (Iterator<? extends AbstractAttribute> attributeItor =
                    schema.getAttributes().iterator();
                    attributeItor.hasNext() && !validationExceptionFound;) {

                attribute = attributeItor.next();
                for (Iterator<? extends AbstractAttributeValue> attributeValueItor =
                        attribute.getValues().iterator();
                        attributeValueItor.hasNext()
                        && !validationExceptionFound;) {

                    attributeValue = attributeValueItor.next();
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
                SyncopeClientCompositeErrorException sccee =
                        new SyncopeClientCompositeErrorException(
                        HttpStatus.BAD_REQUEST);

                SyncopeClientException e = new SyncopeClientException(
                        SyncopeClientExceptionType.InvalidUpdate);

                e.addElement(schema.getName());
                sccee.addException(e);

                throw sccee;
            }

            return schema;
        }

        return null;
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
