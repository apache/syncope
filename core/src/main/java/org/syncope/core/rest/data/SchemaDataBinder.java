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
import org.syncope.client.to.SchemaTO;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.UniqueValueException;

@Component
public class SchemaDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaDataBinder.class);
    private static final String[] ignoreSchemaProperties = {"derivedSchemas",
        "attributes"};
    private SchemaDAO schemaDAO;
    private DerivedSchemaDAO derivedSchemaDAO;

    @Autowired
    public SchemaDataBinder(SchemaDAO schemaDAO,
            DerivedSchemaDAO derivedSchemaDAO) {

        this.schemaDAO = schemaDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
    }

    public <T extends AbstractSchema, K extends AbstractDerivedSchema> T createSchema(
            SchemaTO schemaTO, Class<T> reference, Class<K> derivedReference)
            throws InstantiationException, IllegalAccessException,
            UniqueValueException {

        T schema = reference.newInstance();
        BeanUtils.copyProperties(schemaTO, schema, ignoreSchemaProperties);

        AbstractDerivedSchema abstractDerivedSchema = null;
        for (String derivedSchema : schemaTO.getDerivedSchemas()) {

            abstractDerivedSchema =
                    derivedSchemaDAO.find(derivedSchema, derivedReference);
            if (abstractDerivedSchema != null) {
                schema.addDerivedSchema(abstractDerivedSchema);
            } else {
                log.error("Unmatched derived schema name: " + derivedSchema);
            }
        }

        // Everything went out fine, we can flush to the database
        schema = schemaDAO.save(schema);
        schemaDAO.getEntityManager().flush();
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
