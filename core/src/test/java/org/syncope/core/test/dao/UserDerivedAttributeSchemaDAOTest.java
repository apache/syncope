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
package org.syncope.core.test.dao;

import java.util.List;
import org.junit.Test;
import org.syncope.core.beans.UserDerivedAttributeSchema;
import org.syncope.core.dao.UserDerivedAttributeSchemaDAO;

public class UserDerivedAttributeSchemaDAOTest extends AbstractDAOTest {

    public UserDerivedAttributeSchemaDAOTest() {
        super("userDerivedAttributeSchemaDAO");
    }

    @Override
    protected UserDerivedAttributeSchemaDAO getDAO() {
        return (UserDerivedAttributeSchemaDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<UserDerivedAttributeSchema> list = getDAO().findAll();
        assertEquals("did not get expected number of derived attribute schemas ",
                1, list.size());
    }

    @Test
    public final void testFindByName() {
        UserDerivedAttributeSchema userAttributeSchema =
                getDAO().find("cn");
        assertNotNull("did not find expected derived attribute schema",
                userAttributeSchema);
    }

    @Test
    public final void testSave() {
        UserDerivedAttributeSchema userDerivedAttributeSchema =
                new UserDerivedAttributeSchema();
        userDerivedAttributeSchema.setName("cn2");
        userDerivedAttributeSchema.setExpression("name surname");

        getDAO().save(userDerivedAttributeSchema);

        UserDerivedAttributeSchema actual = getDAO().find("cn2");
        assertNotNull("expected save to work", actual);
        assertEquals(userDerivedAttributeSchema, actual);
    }

    @Test
    public final void testDelete() {
        UserDerivedAttributeSchema userAttributeSchema = getDAO().find("cn");

        getDAO().delete(userAttributeSchema.getName());

        UserDerivedAttributeSchema actual = getDAO().find("cn");
        assertNull("delete did not work", actual);
    }
}
