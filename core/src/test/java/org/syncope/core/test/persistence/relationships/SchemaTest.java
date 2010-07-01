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
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.test.persistence.AbstractTest;

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
    private SchemaMappingDAO schemaMappingDAO;

    @Test
    public final void test1() {
        // 1
        schemaDAO.delete("username", UserSchema.class);

        // 2
        schemaDAO.delete("surname", UserSchema.class);

        schemaDAO.getEntityManager().flush();

        // 1
        assertNull(schemaDAO.find("username", UserSchema.class));
        assertNull(attributeDAO.find(100L, UserAttribute.class));
        assertNull(attributeDAO.find(300L, UserAttribute.class));
        assertNull(syncopeUserDAO.find(1L).getAttribute("username"));
        assertNull(syncopeUserDAO.find(3L).getAttribute("username"));

        // 2
        assertNull(schemaDAO.find("surname", UserSchema.class));
        assertEquals(1, derivedSchemaDAO.find("cn",
                UserDerivedSchema.class).getSchemas().size());
    }

    @Test
    public final void test2() {
        schemaDAO.delete("email", UserSchema.class);

        schemaDAO.getEntityManager().flush();

        assertNull(schemaDAO.find("email", UserSchema.class));
        assertNull(schemaMappingDAO.find(100L));
        assertNull(schemaMappingDAO.find(101L));
    }
}
