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

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UserDerivedAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.AbstractTest;

@Transactional
public class DerivedSchemaTest extends AbstractTest {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private DerivedSchemaDAO derivedSchemaDAO;
    @Autowired
    private AttributeDAO attributeDAO;
    @Autowired
    DerivedAttributeDAO derivedAttributeDAO;

    @Test
    public final void test() {
        derivedSchemaDAO.delete("cn", UserDerivedSchema.class);

        derivedSchemaDAO.flush();

        assertNull(derivedSchemaDAO.find("cn", UserDerivedSchema.class));
        assertNull(derivedAttributeDAO.find(1000L, UserDerivedAttribute.class));
        assertTrue(schemaDAO.find("surname",
                UserSchema.class).getDerivedSchemas().isEmpty());
        assertTrue(schemaDAO.find("firstname",
                UserSchema.class).getDerivedSchemas().isEmpty());
        assertNull(syncopeUserDAO.find(3L).getDerivedAttribute("cn"));
    }
}
