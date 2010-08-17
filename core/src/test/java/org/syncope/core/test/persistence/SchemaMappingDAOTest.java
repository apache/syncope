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
package org.syncope.core.test.persistence;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;

@Transactional
public class SchemaMappingDAOTest extends AbstractTest {

    @Autowired
    private SchemaMappingDAO schemaMappingDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void findById() {
        SchemaMapping schema = schemaMappingDAO.find(100L);

        assertNotNull("findById did not work", schema);

        assertEquals("username", schema.getField());

        assertEquals("email", schema.getUserSchema().getName());

        assertEquals("ws-target-resource-1", schema.getResource().getName());

        assertFalse(schema.isNullable());

        assertTrue(schema.isAccountid());

        assertFalse(schema.isPassword());
    }

    @Test
    public final void save() throws ClassNotFoundException {
        SchemaMapping schema = new SchemaMapping();

        schema.setField("name");
        schema.setUserSchema(schemaDAO.find("firstname", UserSchema.class));
        schema.setResource(resourceDAO.find("ws-target-resource-1"));

        SchemaMapping actual = schemaMappingDAO.save(schema);

        assertNotNull(actual);

        assertTrue(actual.isNullable());

        assertFalse(actual.isAccountid());

        assertFalse(actual.isPassword());

        assertEquals("firstname", actual.getUserSchema().getName());

        assertEquals("name", actual.getField());
    }

    @Test
    public final void delete() {
        SchemaMapping schema = schemaMappingDAO.find(100L);

        assertNotNull("find to delete did not work", schema);

        schemaMappingDAO.delete(schema.getId());

        SchemaMapping actual = schemaMappingDAO.find(100L);

        assertNull("delete did not work", actual);
    }
}
