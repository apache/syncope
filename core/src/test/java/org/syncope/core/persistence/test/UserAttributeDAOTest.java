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
import org.syncope.core.persistence.AttributeType;
import org.syncope.core.persistence.beans.UserAttributeSchema;
import org.syncope.core.persistence.beans.UserAttribute;
import org.syncope.core.persistence.dao.UserAttributeSchemaDAO;
import org.syncope.core.persistence.dao.UserAttributeDAO;
import org.syncope.core.persistence.validation.ValidationException;

@Transactional
public class UserAttributeDAOTest extends AbstractDAOTest {

    @Autowired
    UserAttributeDAO userAttributeDAO;
    @Autowired
    UserAttributeSchemaDAO userAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<UserAttribute> list = userAttributeDAO.findAll();
        assertEquals("did not get expected number of attributes ",
                5, list.size());
    }

    @Test
    public final void testFindById() {
        UserAttribute attribute = userAttributeDAO.find(100L);
        assertNotNull("did not find expected attribute schema",
                attribute);
        attribute = userAttributeDAO.find(200L);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void testSave() throws ClassNotFoundException {
        UserAttributeSchema emailSchema = new UserAttributeSchema();
        emailSchema.setName("email");
        emailSchema.setType(AttributeType.String);
        emailSchema.setValidatorClass(
                "org.syncope.core.persistence.validation.EmailAddressValidator");
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
        if (thrown != null)
            log.error("Validation exception for " + attribute, thrown);

        try {
            attribute.addValue("http://www.apache.org");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute = userAttributeDAO.save(attribute);

        UserAttribute actual = userAttributeDAO.find(attribute.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(attribute, actual);
    }

    @Test
    public final void testDelete() {
        UserAttribute attribute = userAttributeDAO.find(100L);

        userAttributeDAO.delete(attribute.getId());

        UserAttribute actual = userAttributeDAO.find(100L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void testRelationships() {
        UserAttribute attribute = userAttributeDAO.find(200L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        userAttributeDAO.delete(attribute.getId());

        UserAttributeSchema userAttributeSchema =
                userAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
