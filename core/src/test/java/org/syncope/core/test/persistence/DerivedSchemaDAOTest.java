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

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;

@Transactional
public class DerivedSchemaDAOTest extends AbstractTest {

    @Autowired
    DerivedSchemaDAO derivedSchemaDAO;
    @Autowired
    SchemaDAO schemaDAO;

    @Test
    public final void findAll() {
        List<UserDerivedSchema> list =
                derivedSchemaDAO.findAll(UserDerivedSchema.class);
        assertEquals("did not get expected number of derived attribute schemas ",
                1, list.size());
    }

    @Test
    public final void findByName() {
        UserDerivedSchema attributeSchema =
                derivedSchemaDAO.find("cn", UserDerivedSchema.class);
        assertNotNull("did not find expected derived attribute schema",
                attributeSchema);
    }

    @Test
    public final void save() {
        UserDerivedSchema derivedAttributeSchema =
                new UserDerivedSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("firstname surname");
        derivedAttributeSchema.addSchema(
                schemaDAO.find("firstname", UserSchema.class));
        derivedAttributeSchema.addSchema(
                schemaDAO.find("surname", UserSchema.class));

        derivedSchemaDAO.save(derivedAttributeSchema);

        UserDerivedSchema actual =
                derivedSchemaDAO.find("cn2", UserDerivedSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public final void delete() {
        UserDerivedSchema attributeSchema =
                derivedSchemaDAO.find("cn", UserDerivedSchema.class);

        derivedSchemaDAO.delete(attributeSchema.getName(),
                UserDerivedSchema.class);

        UserDerivedSchema actual =
                derivedSchemaDAO.find("cn", UserDerivedSchema.class);
        assertNull("delete did not work", actual);
    }
}
