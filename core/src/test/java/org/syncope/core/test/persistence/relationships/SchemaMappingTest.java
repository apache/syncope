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

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.core.test.persistence.AbstractTest;
import org.syncope.types.SchemaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Transactional
public class SchemaMappingTest extends AbstractTest {

    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void create() throws
            ClassNotFoundException, MultiUniqueValueException {

        SchemaMapping mapping = new SchemaMapping();
        mapping.setSchemaType(SchemaType.UserSchema);
        mapping.setSchemaName("firstname");
        mapping.setField("name");

        TargetResource resource = resourceDAO.find("ws-target-resource-delete");
        assertNotNull(resource);

        // update resource
        resource.addMapping(mapping);
        TargetResource actualResource = resourceDAO.save(resource);

        assertNotNull(actualResource);

        SchemaMapping actualMapping =
                actualResource.getMappings().iterator().next();

        assertNotNull(actualMapping);
        assertTrue(actualMapping.isNullable());
        assertFalse(actualMapping.isAccountid());
        assertFalse(actualMapping.isPassword());
        assertEquals("firstname", actualMapping.getSchemaName());
        assertEquals("name", actualMapping.getField());

        // close the transaction
        schemaDAO.flush();

        actualResource = resourceDAO.find("ws-target-resource-delete");

        assertNotNull(actualResource.getMappings());
        assertFalse(actualResource.getMappings().isEmpty());
        assertTrue(actualResource.getMappings().contains(actualMapping));
    }

    @Test
    public final void delete() throws MultiUniqueValueException {
        SchemaMapping mapping = schemaDAO.findMapping(100L);

        assertNotNull("find did not work", mapping);

        TargetResource resource = mapping.getResource();

        assertNotNull(resource);
        assertNotNull(resource.getMappings());
        assertFalse(resource.getMappings().isEmpty());

        int resourceMappings = resource.getMappings().size();

        assertTrue(resourceMappings > 0);

        UserSchema schema =
                schemaDAO.find(mapping.getSchemaName(), UserSchema.class);

        assertNotNull(schema);
        assertNotNull(schema.getMappings());
        assertFalse(schema.getMappings().isEmpty());

        int userMappings = schema.getMappings().size();

        assertTrue(userMappings > 0);

        schemaDAO.removeMapping(mapping.getId());

        SchemaMapping actual = schemaDAO.findMapping(100L);

        assertNull("delete did not work", actual);

        // close the transaction
        schemaDAO.flush();

        UserSchema actualUser =
                schemaDAO.find(schema.getName(), UserSchema.class);

        assertNotNull(actualUser);
        assertNotNull(actualUser.getMappings());
        assertTrue(userMappings > actualUser.getMappings().size());
        assertFalse(actualUser.getMappings().contains(mapping));

        TargetResource actualResource =
                resourceDAO.find(resource.getName());

        assertNotNull(actualResource);
        assertNotNull(actualResource.getMappings());
        assertTrue(resourceMappings > actualResource.getMappings().size());
        assertFalse(actualResource.getMappings().contains(mapping));
    }

    @Test
    public void update() throws MultiUniqueValueException {
        SchemaMapping mapping = schemaDAO.findMapping(100L);

        assertNotNull(mapping);
        assertEquals("email", mapping.getSchemaName());
        assertEquals(mapping.getSchemaType(), SchemaType.UserSchema);
        assertTrue(mapping.isAccountid());
        assertFalse(mapping.isPassword());
        assertEquals("ws-target-resource-1", mapping.getResource().getName());

        UserSchema schema =
                schemaDAO.find(mapping.getSchemaName(), UserSchema.class);

        assertNotNull(schema);
        assertTrue(schema.getMappings().contains(mapping));

        int schemaMappings = schema.getMappings().size();

        TargetResource resource =
                resourceDAO.find(mapping.getResource().getName());

        assertNotNull(resource);
        assertTrue(resource.getMappings().contains(mapping));

        int resourceMappings = resource.getMappings().size();

        // Schema must be forcely synchronized
        schema.removeMapping(mapping);
        schemaDAO.save(schema);

        // Resource must be forcely synchronized
        resource.removeMapping(mapping);
        resourceDAO.save(resource);

        resource = resourceDAO.find("ws-target-resource-2");

        mapping.setAccountid(false);
        mapping.setPassword(true);
        mapping.setSchemaName("Password");
        mapping.setSchemaType(SchemaType.Password);
        mapping.setResource(resource);

        SchemaMapping actual = schemaDAO.saveMapping(mapping);
        schemaDAO.flush();

        assertNotNull(actual);
        assertEquals("Password", actual.getSchemaName());
        assertEquals(actual.getSchemaType(), SchemaType.Password);
        assertFalse(actual.isAccountid());
        assertTrue(actual.isPassword());
        assertEquals("ws-target-resource-2", actual.getResource().getName());

        // Check for synchronization

        schema = schemaDAO.find("email", UserSchema.class);
        assertNotNull(schema);
        assertTrue(schemaMappings > schema.getMappings().size());

        resource = resourceDAO.find("ws-target-resource-1");
        assertNotNull(resource);
        assertTrue(resourceMappings > resource.getMappings().size());
    }

    @Test
    public void removeResourceAndCheckForMapping() {
        TargetResource resource = resourceDAO.find("ws-target-resource-2");

        assertNotNull(resource);

        List<SchemaMapping> mappings = resource.getMappings();

        assertNotNull(mappings);
        assertFalse(mappings.isEmpty());

        Set<Long> mappingIds = new HashSet<Long>();
        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }

        resourceDAO.delete("ws-target-resource-2");

        resourceDAO.flush();

        resource = resourceDAO.find("ws-target-resource-2");

        assertNull(resource);

        for (Long id : mappingIds) {
            assertNull(schemaDAO.findMapping(id));
        }
    }

    @Test
    public void removeSchemaAndCheckForMapping() {
        UserSchema schema = schemaDAO.find("email", UserSchema.class);

        assertNotNull(schema);

        List<SchemaMapping> mappings = schema.getMappings();

        assertNotNull(mappings);
        assertFalse(mappings.isEmpty());

        Set<Long> mappingIds = new HashSet<Long>();
        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }

        schemaDAO.delete("email", UserSchema.class);

        schemaDAO.flush();

        schema = schemaDAO.find("email", UserSchema.class);

        assertNull(schema);

        for (Long id : mappingIds) {
            assertNull(schemaDAO.findMapping(id));
        }
    }

    @Test
    public void checkForAccountId() {
        schemaDAO.removeMapping(99L);
        schemaDAO.flush();

        assertNull(schemaDAO.findMapping(99L));
    }

    @Test
    public void checkForPassword() {
        schemaDAO.removeMapping(106L);
        schemaDAO.flush();

        assertNull(schemaDAO.findMapping(106L));
    }
}
