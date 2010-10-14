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
package org.syncope.core.persistence.relationships;

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.types.SchemaType;

@Transactional
public class SchemaTest extends AbstractTest {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void test1() {
        // search for user schema username
        UserSchema schema = schemaDAO.find("username", UserSchema.class);

        assertNotNull(schema);

        // check for associated mappings
        List<SchemaMapping> mappings = resourceDAO.getMappings(
                schema.getName(),
                SchemaType.UserSchema);
        assertFalse(mappings.isEmpty());

        // delete user schema username
        schemaDAO.delete("username", UserSchema.class);

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("username", UserSchema.class);

        assertNull(schema);

        // check for mappings deletion
        mappings = resourceDAO.getMappings("username",
                SchemaType.UserSchema);
        assertTrue(mappings.isEmpty());

        assertNull(attributeDAO.find(100L, UserAttribute.class));
        assertNull(attributeDAO.find(300L, UserAttribute.class));
        assertNull(syncopeUserDAO.find(1L).getAttribute("username"));
        assertNull(syncopeUserDAO.find(3L).getAttribute("username"));
    }

    @Test
    public void test2() {

        // search for user schema username
        UserSchema schema = schemaDAO.find("surname", UserSchema.class);

        assertNotNull(schema);

        // check for associated mappings
        List<SchemaMapping> mappings = resourceDAO.getMappings(
                schema.getName(),
                SchemaType.UserSchema);
        assertNotNull(mappings);

        // delete user schema username
        schemaDAO.delete("surname", UserSchema.class);

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("surname", UserSchema.class);

        assertNull(schema);

        assertNull(schemaDAO.find("surname", UserSchema.class));

        assertEquals(1, derivedSchemaDAO.find("cn",
                UserDerivedSchema.class).getSchemas().size());
    }
}
