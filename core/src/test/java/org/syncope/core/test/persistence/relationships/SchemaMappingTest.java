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
package org.syncope.core.test.persistence.relationships;

import java.util.List;
import org.syncope.core.test.persistence.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;

@Transactional
public class SchemaMappingTest extends AbstractTest {

    @Autowired
    private SchemaMappingDAO schemaMappingDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void save() throws ClassNotFoundException {
        SchemaMapping schema = new SchemaMapping();

        schema.setField("name");

        UserSchema user = schemaDAO.find("firstname", UserSchema.class);
        schema.setUserSchema(user);

        TargetResource resource = resourceDAO.find("ws-target-resource-1");
        schema.setResource(resource);

        SchemaMapping actual = schemaMappingDAO.save(schema);

        user.addMapping(actual);
        resource.addMapping(actual);

        // close the transaction
        schemaMappingDAO.flush();

        assertNotNull(actual);

        assertTrue(actual.isNullable());

        assertFalse(actual.isAccountid());

        assertFalse(actual.isPassword());

        assertEquals("firstname", actual.getUserSchema().getName());

        assertEquals("name", actual.getField());

        UserSchema actualUser =
                schemaDAO.find("firstname", UserSchema.class);

        assertTrue(actualUser.getMappings().contains(actual));

        TargetResource actualResource =
                resourceDAO.find("ws-target-resource-1");

        assertTrue(actualResource.getMappings().contains(actual));
    }

    @Test
    public final void delete() {
        SchemaMapping schema = schemaMappingDAO.find(100L);

        assertNotNull("find did not work", schema);

        Long id = schema.getId();

        TargetResource resource = schema.getResource();

        assertNotNull(resource);

        UserSchema user = schema.getUserSchema();

        assertNotNull(user);

        schemaMappingDAO.delete(schema.getId());

        // close the transaction
        schemaMappingDAO.flush();

        SchemaMapping actual = schemaMappingDAO.find(100L);

        assertNull("delete did not work", actual);

        TargetResource actualResource =
                resourceDAO.find(resource.getName());

        assertNotNull(actualResource);

        List<SchemaMapping> mappings = actualResource.getMappings();
        if (mappings != null) {
            for (SchemaMapping mapping : mappings) {
                assertFalse(mapping.getId().equals(id));
            }
        }

        UserSchema actualUser =
                schemaDAO.find(user.getName(), UserSchema.class);

        assertNotNull(actualUser);

        mappings = actualUser.getMappings();
        if (mappings != null) {
            for (SchemaMapping mapping : mappings) {
                assertFalse(mapping.getId().equals(id));
            }
        }
    }
}
