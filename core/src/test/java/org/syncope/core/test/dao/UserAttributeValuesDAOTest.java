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
import org.springframework.context.ApplicationContext;
import org.syncope.core.beans.UserAttributeSchema;
import org.syncope.core.beans.UserAttributeValues;
import org.syncope.core.dao.UserAttributeSchemaDAO;
import org.syncope.core.dao.UserAttributeValuesDAO;
import org.syncope.core.enums.AttributeType;

public class UserAttributeValuesDAOTest extends AbstractDAOTest {

    UserAttributeSchemaDAO userAttributeSchemaDAO;

    public UserAttributeValuesDAOTest() {
        super("userAttributeValuesDAO", "UserAttributeValuesDAOImpl");

        ApplicationContext ctx = super.getApplicationContext();
        userAttributeSchemaDAO = (UserAttributeSchemaDAO) ctx.getBean("userAttributeSchemaDAO");
        assertNotNull(userAttributeSchemaDAO);
    }

    @Override
    protected UserAttributeValuesDAO getDAO() {
        return (UserAttributeValuesDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<UserAttributeValues> list = getDAO().findAll();
        assertEquals("did not get expected number of attribute schemas ",
                2, list.size());
    }

    @Test
    public final void testFindByName() {
        UserAttributeValues userAttributeValues = getDAO().find(100L);
        assertNotNull("did not find expected attribute schema",
                userAttributeValues);
        userAttributeValues = getDAO().find(200L);
        assertNotNull("did not find expected attribute schema",
                userAttributeValues);
    }

    @Test
    public final void testSave() throws ClassNotFoundException {
        UserAttributeSchema emailSchema = new UserAttributeSchema();
        emailSchema.setName("email");
        emailSchema.setType(AttributeType.String);
        emailSchema.setMandatory(false);
        emailSchema.setMultivalue(true);

        userAttributeSchemaDAO.save(emailSchema);

        UserAttributeSchema actualEmailSchema =
                userAttributeSchemaDAO.find("email");
        assertNotNull("expected save to work for e-mail schema",
                actualEmailSchema);

        UserAttributeValues userAttributeValues =
                new UserAttributeValues(actualEmailSchema);
        userAttributeValues.addAttributeValue("john.doe@gmail.com");
        userAttributeValues.addAttributeValue("mario.rossi@gmail.com");

        userAttributeValues = getDAO().save(userAttributeValues);

        UserAttributeValues actual = getDAO().find(userAttributeValues.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(userAttributeValues, actual);
    }

    @Test
    public final void testDelete() {

        UserAttributeValues userAttributeValues = getDAO().find(100L);

        getDAO().delete(userAttributeValues.getId());

        UserAttributeValues actual = getDAO().find(100L);
        assertNull("delete did not work", actual);
    }
}
