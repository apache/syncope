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
import org.syncope.core.persistence.beans.UserDerivedAttributeSchema;
import org.syncope.core.persistence.dao.UserDerivedAttributeSchemaDAO;

@Transactional
public class UserDerivedAttributeSchemaDAOTest extends AbstractDAOTest {

    @Autowired
    UserDerivedAttributeSchemaDAO userDerivedAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<UserDerivedAttributeSchema> list =
                userDerivedAttributeSchemaDAO.findAll();
        assertEquals("did not get expected number of derived attribute schemas ",
                1, list.size());
    }

    @Test
    public final void testFindByName() {
        UserDerivedAttributeSchema userAttributeSchema =
                userDerivedAttributeSchemaDAO.find("cn");
        assertNotNull("did not find expected derived attribute schema",
                userAttributeSchema);
    }

    @Test
    public final void testSave() {
        UserDerivedAttributeSchema userDerivedAttributeSchema =
                new UserDerivedAttributeSchema();
        userDerivedAttributeSchema.setName("cn2");
        userDerivedAttributeSchema.setExpression("name surname");

        userDerivedAttributeSchemaDAO.save(userDerivedAttributeSchema);

        UserDerivedAttributeSchema actual =
                userDerivedAttributeSchemaDAO.find("cn2");
        assertNotNull("expected save to work", actual);
        assertEquals(userDerivedAttributeSchema, actual);
    }

    @Test
    public final void testDelete() {
        UserDerivedAttributeSchema userAttributeSchema =
                userDerivedAttributeSchemaDAO.find("cn");

        userDerivedAttributeSchemaDAO.delete(userAttributeSchema.getName());

        UserDerivedAttributeSchema actual =
                userDerivedAttributeSchemaDAO.find("cn");
        assertNull("delete did not work", actual);
    }
}
