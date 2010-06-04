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
import org.syncope.core.AttributeType;
import org.syncope.core.beans.UserAttributeSchema;
import org.syncope.core.beans.UserAttribute;
import org.syncope.core.dao.UserAttributeSchemaDAO;
import org.syncope.core.dao.UserAttributeDAO;
import org.syncope.core.validation.ValidationException;

public class UserAttributeDAOTest extends AbstractDAOTest {

    UserAttributeSchemaDAO userAttributeSchemaDAO;

    public UserAttributeDAOTest() {
        super("userAttributeDAO");

        ApplicationContext ctx = super.getApplicationContext();
        userAttributeSchemaDAO =
                (UserAttributeSchemaDAO) ctx.getBean("userAttributeSchemaDAO");
        assertNotNull(userAttributeSchemaDAO);
    }

    @Override
    protected UserAttributeDAO getDAO() {
        return (UserAttributeDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<UserAttribute> list = getDAO().findAll();
        assertEquals("did not get expected number of attributes ",
                5, list.size());
    }

    @Test
    public final void testFindById() {
        UserAttribute attribute = getDAO().find(100L);
        assertNotNull("did not find expected attribute schema",
                attribute);
        attribute = getDAO().find(200L);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void testSave() throws ClassNotFoundException {
        UserAttributeSchema emailSchema = new UserAttributeSchema();
        emailSchema.setName("email");
        emailSchema.setType(AttributeType.String);
        emailSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        emailSchema.setMandatory(false);
        emailSchema.setMultivalue(true);

        userAttributeSchemaDAO.save(emailSchema);

        UserAttributeSchema actualEmailSchema =
                userAttributeSchemaDAO.find("email");
        assertNotNull("expected save to work for e-mail schema",
                actualEmailSchema);

        UserAttribute attribute =
                new UserAttribute(actualEmailSchema);

        Exception thrown = null;
        try {
            attribute.addValue("john.doe@gmail.com");
            attribute.addValue("mario.rossi@gmail.com");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);

        try {
            attribute.addValue("http://www.apache.org");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute = getDAO().save(attribute);

        UserAttribute actual = getDAO().find(attribute.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(attribute, actual);
    }

    @Test
    public final void testDelete() {
        UserAttribute attribute = getDAO().find(100L);

        getDAO().delete(attribute.getId());

        UserAttribute actual = getDAO().find(100L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void testRelationships() {
        UserAttribute attribute = getDAO().find(200L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        getDAO().delete(attribute.getId());

        UserAttributeSchema userAttributeSchema =
                userAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
