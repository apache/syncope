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
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.Attribute;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.validation.ValidationException;

@Transactional
public class AttributeDAOTest extends AbstractDAOTest {

    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    AttributeSchemaDAO attributeSchemaDAO;

    @Test
    public final void findAll() {
        List<Attribute> list = attributeDAO.findAll();
        assertEquals("did not get expected number of attributes ",
                8, list.size());
    }

    @Test
    public final void findById() {
        Attribute attribute = attributeDAO.find(100L);
        assertNotNull("did not find expected attribute schema",
                attribute);
        attribute = attributeDAO.find(200L);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        AttributeSchema emailSchema = new AttributeSchema();
        emailSchema.setName("email");
        emailSchema.setType(AttributeType.String);
        emailSchema.setValidatorClass(
                "org.syncope.core.persistence.validation.EmailAddressValidator");
        emailSchema.setMandatory(false);
        emailSchema.setMultivalue(true);

        attributeSchemaDAO.save(emailSchema);

        AttributeSchema actualEmailSchema =
                attributeSchemaDAO.find("email");
        assertNotNull("expected save to work for e-mail schema",
                actualEmailSchema);

        Attribute attribute =
                new Attribute(actualEmailSchema);

        Exception thrown = null;
        try {
            attribute.addValue("john.doe@gmail.com");
            attribute.addValue("mario.rossi@gmail.com");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);
        if (thrown != null) {
            log.error("Validation exception for " + attribute, thrown);
        }

        try {
            attribute.addValue("http://www.apache.org");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute = attributeDAO.save(attribute);

        Attribute actual = attributeDAO.find(attribute.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(attribute, actual);
    }

    @Test
    public final void delete() {
        Attribute attribute = attributeDAO.find(200L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        attributeDAO.delete(attribute.getId());

        AttributeSchema attributeSchema =
                attributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user attribute schema deleted when deleting values",
                attributeSchema);
    }
}
