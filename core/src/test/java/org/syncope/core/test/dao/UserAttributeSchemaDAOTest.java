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
import org.syncope.core.beans.UserAttributeSchema;
import org.syncope.core.dao.UserAttributeSchemaDAO;
import org.syncope.core.enums.AttributeType;

public class UserAttributeSchemaDAOTest extends AbstractDAOTest {

    public UserAttributeSchemaDAOTest() {
        super("userAttributeSchemaDAO", "UserAttributeSchemaDAOImpl");
    }

    @Override
    protected UserAttributeSchemaDAO getDAO() {
        return (UserAttributeSchemaDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<UserAttributeSchema> list = getDAO().findAll();
        assertEquals("did not get expected number of attribute schemas ",
                2, list.size());
    }

    @Test
    public final void testFindByName() {
        UserAttributeSchema userAttributeSchema = getDAO().find("username");
        assertNotNull("did not find expected attribute schema",
                userAttributeSchema);
        userAttributeSchema = getDAO().find("birthdate");
        assertNotNull("did not find expected attribute schema",
                userAttributeSchema);
    }

    @Test
    public final void testSave() {
        UserAttributeSchema userAttributeSchema = new UserAttributeSchema();
        userAttributeSchema.setName("email");
        userAttributeSchema.setType(AttributeType.String);

        getDAO().save(userAttributeSchema);

        UserAttributeSchema actual = getDAO().find("email");
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        UserAttributeSchema userAttributeSchema = getDAO().find("username");

        getDAO().delete(userAttributeSchema.getName());

        UserAttributeSchema actual = getDAO().find("username");
        assertNull("delete did not work", actual);
    }
}
