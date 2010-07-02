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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;

@Component
public class DerivedSchemaDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            DerivedSchemaDataBinder.class);
    private static final String[] ignoreDerivedSchemaProperties = {"schemas",
        "derivedAttributes"};
    private SchemaDAO schemaDAO;
    private DerivedSchemaDAO derivedSchemaDAO;

    @Autowired
    public DerivedSchemaDataBinder(SchemaDAO schemaDAO,
            DerivedSchemaDAO derivedSchemaDAO) {

        this.schemaDAO = schemaDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
    }

    private <T extends AbstractDerivedSchema, K extends AbstractSchema> T populateDerivedSchema(
            T derivedSchema,
            DerivedSchemaTO derivedSchemaTO,
            Class<K> reference) {

        BeanUtils.copyProperties(derivedSchemaTO, derivedSchema,
                ignoreDerivedSchemaProperties);

        AbstractSchema abstractSchema = null;
        for (String schema : derivedSchemaTO.getSchemas()) {

            abstractSchema = schemaDAO.find(schema, reference);
            if (abstractSchema != null) {
                derivedSchema.addSchema(abstractSchema);
            } else {
                log.error("Unmatched schema name: " + schema);
            }
        }

        return derivedSchema;
    }

    public <T extends AbstractDerivedSchema, K extends AbstractSchema> T createDerivedSchema(
            DerivedSchemaTO derivedSchemaTO,
            Class<T> derivedReference,
            Class<K> reference)
            throws InstantiationException, IllegalAccessException {

        T derivedSchema = populateDerivedSchema(derivedReference.newInstance(),
                derivedSchemaTO, reference);

        // Everything went out fine, we can flush to the database
        return derivedSchemaDAO.save(derivedSchema);
    }

    public <T extends AbstractDerivedSchema, K extends AbstractSchema> T updateDerivedSchema(
            DerivedSchemaTO derivedSchemaTO,
            Class<T> derivedReference,
            Class<K> reference) {

        T derivedSchema = derivedSchemaDAO.find(derivedSchemaTO.getName(),
                derivedReference);
        if (derivedSchema != null) {
            derivedSchema = populateDerivedSchema(derivedSchema, derivedSchemaTO, reference);

            // Everything went out fine, we can flush to the database
            return derivedSchemaDAO.save(derivedSchema);
        }

        return null;
    }

    public <T extends AbstractDerivedSchema> DerivedSchemaTO getDerivedSchemaTO(
            T derivedSchema) {

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
