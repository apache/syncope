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
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.AttributeType;

@Transactional
public class AttributeDAOTest extends AbstractTest {

    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    SchemaDAO userSchemaDAO;

    @Test
    public final void findAll() {
        List<UserAttribute> list = attributeDAO.findAll(UserAttribute.class);
        assertEquals("did not get expected number of attributes ",
                5, list.size());
    }

    @Test
    public final void findById() {
        UserAttribute attribute = attributeDAO.find(100L, UserAttribute.class);
        assertNotNull("did not find expected attribute schema",
                attribute);
        attribute = attributeDAO.find(200L, UserAttribute.class);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        UserSchema emailSchema = new UserSchema();
        emailSchema.setName("email");
        emailSchema.setType(AttributeType.String);
        emailSchema.setValidatorClass(
                "org.syncope.core.persistence.validation.EmailAddressValidator");
        emailSchema.setMandatory(false);
        emailSchema.setMultivalue(true);

        userSchemaDAO.save(emailSchema);

        UserSchema actualEmailSchema = userSchemaDAO.find("email",
                UserSchema.class);
        assertNotNull("expected save to work for e-mail schema",
                actualEmailSchema);

        UserAttribute attribute = new UserAttribute();
        attribute.setSchema(actualEmailSchema);

        Exception thrown = null;
        try {
            attribute.addValue("john.doe@gmail.com",
                    new UserAttributeValue());
            attribute.addValue("mario.rossi@gmail.com",
                    new UserAttributeValue());
        } catch (ValidationException e) {
            e.printStackTrace();
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);
        if (thrown != null) {
            log.error("Validation exception for " + attribute, thrown);
        }

        try {
            attribute.addValue("http://www.apache.org",
                    new UserAttributeValue());
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute = attributeDAO.save(attribute);

        UserAttribute actual = attributeDAO.find(attribute.getId(),
                UserAttribute.class);
        assertNotNull("expected save to work", actual);
        assertEquals(attribute, actual);
    }

    @Test
    public final void delete() {
        UserAttribute attribute = attributeDAO.find(200L, UserAttribute.class);
        String attributeSchemaName =
                attribute.getSchema().getName();

        attributeDAO.delete(attribute.getId(), UserAttribute.class);

        UserSchema attributeSchema =
                userSchemaDAO.find(attributeSchemaName, UserSchema.class);
        assertNotNull("user attribute schema deleted when deleting values",
                attributeSchema);
    }
}
