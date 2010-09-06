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

import java.util.List;
import org.junit.Before;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.membership.MembershipSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.types.SchemaType;

@Transactional
public class SchemaMappingDAOTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Before
    public final void checkBeforeForStoredData() {
        List<SchemaMapping> mappings = schemaDAO.findAllMappings();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found " + mappings);
        }

        assertNotNull(mappings);
        assertFalse(mappings.isEmpty());

        for (SchemaMapping mapping : mappings) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Check for schema mapping " + mapping);
            }

            String name = mapping.getSchemaName();
            assertNotNull(name);

            SchemaType type = mapping.getSchemaType();
            assertNotNull(type);

            TargetResource resource = mapping.getResource();
            assertNotNull(resource);

            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "\nRelated schema name: " + name +
                        "\nRelated schema type: " + type.toString() +
                        "\nRelated resource name: " + resource.getName() +
                        "\nBrothers in resource : " + resource.getMappings());
            }

            AbstractSchema schema = null;

            schema = schemaDAO.find(name, UserSchema.class);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Brothers in UserSchema: " +
                        (schema != null ? ((UserSchema)schema).getMappings() : ""));
            }

            schema = schemaDAO.find(name, RoleSchema.class);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Brothers in RoleSchema: " +
                        (schema != null ? ((RoleSchema)schema).getMappings() : ""));
            }

            schema = schemaDAO.find(name, MembershipSchema.class);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Brothers in MembershipSchema: " +
                        (schema != null ? ((MembershipSchema)schema).getMappings() : ""));
            }
        }
    }

    @Test
    public final void findById() {
        SchemaMapping schema = schemaDAO.findMapping(100L);

        assertNotNull("findById did not work", schema);

        assertEquals("email", schema.getField());

        assertEquals("email", schema.getSchemaName());

        assertEquals("ws-target-resource-1", schema.getResource().getName());

        assertFalse(schema.isNullable());

        assertTrue(schema.isAccountid());

        assertFalse(schema.isPassword());
    }

    @Test
    public final void save() throws ClassNotFoundException {
        SchemaMapping schema = new SchemaMapping();

        schema.setField("name");
        schema.setSchemaName("firstname");
        schema.setSchemaType(SchemaType.UserSchema);
        schema.setResource(resourceDAO.find("ws-target-resource-1"));

        SchemaMapping actual = schemaDAO.saveMapping(schema);

        assertNotNull(actual);

        assertTrue(actual.isNullable());

        assertFalse(actual.isAccountid());

        assertFalse(actual.isPassword());

        assertEquals("firstname", actual.getSchemaName());

        assertEquals("name", actual.getField());
    }

    @Test
    public final void delete() {
        SchemaMapping mapping = schemaDAO.findMapping(100L);

        assertNotNull("find to delete did not work", mapping);

        schemaDAO.removeMapping(mapping.getId());

        SchemaMapping actual = schemaDAO.findMapping(100L);

        assertNull("delete did not work", actual);
    }
}
