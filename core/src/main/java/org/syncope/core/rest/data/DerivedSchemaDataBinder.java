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

import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
@Transactional(rollbackFor = {Throwable.class})
public class DerivedSchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            DerivedSchemaDataBinder.class);
    private static final String[] ignoreDerivedSchemaProperties = {
        "schemas", "derivedAttributes"};
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private JexlEngine jexlEngine;

    private <T extends AbstractSchema> AbstractDerivedSchema populate(
            AbstractDerivedSchema derivedSchema,
            final DerivedSchemaTO derivedSchemaTO,
            final Class<T> reference,
            final SyncopeClientCompositeErrorException scce)
            throws SyncopeClientCompositeErrorException {

        if (derivedSchemaTO.getExpression() == null) {
            SyncopeClientException requiredValuesMissing =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
            requiredValuesMissing.addElement("expression");

            scce.addException(requiredValuesMissing);
        }

        try {
            jexlEngine.createExpression(derivedSchemaTO.getExpression());
        } catch (JexlException e) {
            LOG.error("Invalid derived schema expression: "
                    + derivedSchemaTO.getExpression(), e);

            SyncopeClientException invalidMandatoryCondition =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidValues);
            invalidMandatoryCondition.addElement(
                    derivedSchemaTO.getExpression());

            scce.addException(invalidMandatoryCondition);
        }

        if (scce.hasExceptions()) {
            throw scce;
        }

        BeanUtils.copyProperties(derivedSchemaTO, derivedSchema,
                ignoreDerivedSchemaProperties);

        AbstractSchema abstractSchema = null;
        for (String schema : derivedSchemaTO.getSchemas()) {
            abstractSchema = schemaDAO.find(schema, reference);
            if (abstractSchema != null) {
                derivedSchema.addSchema(abstractSchema);
            } else {
                LOG.error("Unmatched schema name: " + schema);
            }
        }

        return derivedSchema;
    }

    public <T extends AbstractSchema> AbstractDerivedSchema create(
            final DerivedSchemaTO derivedSchemaTO,
            AbstractDerivedSchema derivedSchema,
            final Class<T> reference) {

        return populate(derivedSchema, derivedSchemaTO, reference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <K extends AbstractSchema> AbstractDerivedSchema update(
            final DerivedSchemaTO derivedSchemaTO,
            AbstractDerivedSchema derivedSchema,
            final Class<K> reference) {

        return populate(derivedSchema, derivedSchemaTO, reference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <T extends AbstractDerivedSchema> DerivedSchemaTO getDerivedSchemaTO(
            final T derivedSchema) {

        DerivedSchemaTO derivedSchemaTO = new DerivedSchemaTO();
        BeanUtils.copyProperties(derivedSchema, derivedSchemaTO,
                ignoreDerivedSchemaProperties);

        for (AbstractSchema schema : derivedSchema.getSchemas()) {

            derivedSchemaTO.addSchema(schema.getName());
        }
        derivedSchemaTO.setDerivedAttributes(
                derivedSchema.getDerivedAttributes().size());

        return derivedSchemaTO;
    }
}
