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
import org.syncope.core.persistence.beans.SyncopeUser;
import org.syncope.core.persistence.beans.AttributeValueAsString;
import org.syncope.core.persistence.beans.DerivedAttribute;
import org.syncope.core.persistence.beans.DerivedAttributeSchema;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedAttributeSchemaDAO;

@Transactional
public class DerivedAttributeDAOTest extends AbstractDAOTest {

    @Autowired
    DerivedAttributeDAO derivedAttributeDAO;
    @Autowired
    SyncopeUserDAO syncopeUserDAO;
    @Autowired
    DerivedAttributeSchemaDAO derivedAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<DerivedAttribute> list = derivedAttributeDAO.findAll();
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void testFindById() {
        DerivedAttribute attribute = derivedAttributeDAO.find(1000L);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void testSave() throws ClassNotFoundException {
        DerivedAttributeSchema derivedAttributeSchema =
                new DerivedAttributeSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("firstname + \" \" + surname");

        derivedAttributeSchemaDAO.save(derivedAttributeSchema);

        DerivedAttributeSchema actualCN2Schema =
                derivedAttributeSchemaDAO.find("cn2");
        assertNotNull("expected save to work for CN2 schema",
                actualCN2Schema);

        SyncopeUser owner = syncopeUserDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        DerivedAttribute derivedAttribute = new DerivedAttribute();
        derivedAttribute.setSchema(derivedAttributeSchema);

        derivedAttribute = derivedAttributeDAO.save(derivedAttribute);

        DerivedAttribute actual = derivedAttributeDAO.find(derivedAttribute.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        AttributeValueAsString firstnameAttribute =
                (AttributeValueAsString) owner.getAttribute(
                "firstname").getValues().iterator().next();
        AttributeValueAsString surnameAttribute =
                (AttributeValueAsString) owner.getAttribute(
                "surname").getValues().iterator().next();

        assertEquals("expected derived value",
                firstnameAttribute.getActualValue() + " "
                + surnameAttribute.getActualValue(),
                derivedAttribute.getValue(owner.getAttributes()));
    }

    @Test
    public final void testDeleteAndRelationships() {
        DerivedAttribute attribute = derivedAttributeDAO.find(1000L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        derivedAttributeDAO.delete(attribute.getId());

        DerivedAttribute actual = derivedAttributeDAO.find(1000L);
        assertNull("delete did not work", actual);

        DerivedAttributeSchema attributeSchema =
                derivedAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user derived attribute schema deleted when deleting values",
                attributeSchema);
    }
}
