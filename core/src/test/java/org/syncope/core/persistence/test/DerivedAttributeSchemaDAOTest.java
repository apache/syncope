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
import org.syncope.core.persistence.beans.DerivedAttributeSchema;
import org.syncope.core.persistence.dao.DerivedAttributeSchemaDAO;

@Transactional
public class DerivedAttributeSchemaDAOTest extends AbstractDAOTest {

    @Autowired
    DerivedAttributeSchemaDAO derivedAttributeSchemaDAO;

    @Test
    public final void findAll() {
        List<DerivedAttributeSchema> list =
                derivedAttributeSchemaDAO.findAll();
        assertEquals("did not get expected number of derived attribute schemas ",
                1, list.size());
    }

    @Test
    public final void findByName() {
        DerivedAttributeSchema attributeSchema =
                derivedAttributeSchemaDAO.find("cn");
        assertNotNull("did not find expected derived attribute schema",
                attributeSchema);
    }

    @Test
    public final void save() {
        DerivedAttributeSchema derivedAttributeSchema =
                new DerivedAttributeSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("name surname");

        derivedAttributeSchemaDAO.save(derivedAttributeSchema);

        DerivedAttributeSchema actual =
                derivedAttributeSchemaDAO.find("cn2");
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttributeSchema, actual);
    }

    @Test
    public final void delete() {
        DerivedAttributeSchema attributeSchema =
                derivedAttributeSchemaDAO.find("cn");

        derivedAttributeSchemaDAO.delete(attributeSchema.getName());

        DerivedAttributeSchema actual =
                derivedAttributeSchemaDAO.find("cn");
        assertNull("delete did not work", actual);
    }
}
