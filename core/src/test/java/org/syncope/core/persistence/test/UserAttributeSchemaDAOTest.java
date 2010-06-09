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
package org.syncope.core.persistence.test;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.UserAttributeSchema;
import org.syncope.core.persistence.dao.UserAttributeSchemaDAO;
import org.syncope.core.persistence.AttributeType;

@Transactional
public class UserAttributeSchemaDAOTest extends AbstractDAOTest {

    @Autowired
    UserAttributeSchemaDAO userAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<UserAttributeSchema> list = userAttributeSchemaDAO.findAll();
        assertEquals("did not get expected number of attribute schemas ",
                4, list.size());
    }

    @Test
    public final void testFindByName() {
        UserAttributeSchema userAttributeSchema = 
                userAttributeSchemaDAO.find("username");
        assertNotNull("did not find expected attribute schema",
                userAttributeSchema);
    }

    @Test
    public final void testSave() {
        UserAttributeSchema userAttributeSchema = new UserAttributeSchema();
        userAttributeSchema.setName("email");
        userAttributeSchema.setType(AttributeType.String);
        userAttributeSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        userAttributeSchema.setMandatory(false);
        userAttributeSchema.setMultivalue(true);

        userAttributeSchemaDAO.save(userAttributeSchema);

        UserAttributeSchema actual = userAttributeSchemaDAO.find("email");
        assertNotNull("expected save to work", actual);
        assertEquals(userAttributeSchema, actual);
    }

    @Test
    public final void testDelete() {
        UserAttributeSchema userAttributeSchema = 
                userAttributeSchemaDAO.find("username");

        userAttributeSchemaDAO.delete(userAttributeSchema.getName());

        UserAttributeSchema actual = userAttributeSchemaDAO.find("username");
        assertNull("delete did not work", actual);
    }
}
