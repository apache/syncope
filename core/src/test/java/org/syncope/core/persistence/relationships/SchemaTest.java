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

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.util.AttributableUtil;
import org.syncope.types.SourceMappingType;

@Transactional
public class SchemaTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public final void test1() {
        // search for user schema username
        USchema schema = schemaDAO.find("username", USchema.class);

        assertNotNull(schema);

        // check for associated mappings
        Set<SchemaMapping> mappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (schema.getName().equals(mapping.getSourceAttrName())
                    && mapping.getSourceMappingType()
                    == SourceMappingType.UserSchema) {

                mappings.add(mapping);
            }
        }
        assertFalse(mappings.isEmpty());

        // delete user schema username
        schemaDAO.delete("username", AttributableUtil.USER);

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("username", USchema.class);

        assertNull(schema);

        // check for mappings deletion
        mappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if ("username".equals(mapping.getSourceAttrName())
                    && mapping.getSourceMappingType()
                    == SourceMappingType.UserSchema) {

                mappings.add(mapping);
            }
        }
        assertTrue(mappings.isEmpty());

        assertNull(attrDAO.find(100L, UAttr.class));
        assertNull(attrDAO.find(300L, UAttr.class));
        assertNull(userDAO.find(1L).getAttribute("username"));
        assertNull(userDAO.find(3L).getAttribute("username"));
    }

    @Test
    public void test2() {

        // search for user schema username
        USchema schema = schemaDAO.find("surname", USchema.class);

        assertNotNull(schema);

        // check for associated mappings
        Set<SchemaMapping> mappings = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
            if (schema.getName().equals(mapping.getSourceAttrName())
                    && mapping.getSourceMappingType()
                    == SourceMappingType.UserSchema) {

                mappings.add(mapping);
            }
        }
        assertFalse(mappings.isEmpty());

        // delete user schema username
        schemaDAO.delete("surname", AttributableUtil.USER);

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("surname", USchema.class);

        assertNull(schema);

        assertNull(schemaDAO.find("surname", USchema.class));
    }
}
