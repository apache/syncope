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
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;
import org.syncope.core.persistence.AttributeType;

@Transactional
public class AttributeSchemaDAOTest extends AbstractDAOTest {

    @Autowired
    AttributeSchemaDAO attributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<AttributeSchema> list = attributeSchemaDAO.findAll();
        assertEquals("did not get expected number of attribute schemas ",
                4, list.size());
    }

    @Test
    public final void testFindByName() {
        AttributeSchema attributeSchema =
                attributeSchemaDAO.find("username");
        assertNotNull("did not find expected attribute schema",
                attributeSchema);
    }

    @Test
    public final void testSave() {
        AttributeSchema attributeSchema = new AttributeSchema();
        attributeSchema.setName("email");
        attributeSchema.setType(AttributeType.String);
        attributeSchema.setValidatorClass(
                "org.syncope.core.validation.EmailAddressValidator");
        attributeSchema.setMandatory(false);
        attributeSchema.setMultivalue(true);

        attributeSchemaDAO.save(attributeSchema);

        AttributeSchema actual = attributeSchemaDAO.find("email");
        assertNotNull("expected save to work", actual);
        assertEquals(attributeSchema, actual);
    }

    @Test
    public final void testDelete() {
        AttributeSchema attributeSchema =
                attributeSchemaDAO.find("username");

        attributeSchemaDAO.delete(attributeSchema.getName());

        AttributeSchema actual = attributeSchemaDAO.find("username");
        assertNull("delete did not work", actual);
    }
}
