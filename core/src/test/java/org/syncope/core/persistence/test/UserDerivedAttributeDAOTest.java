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
import org.syncope.core.persistence.beans.UserAttributeValueAsString;
import org.syncope.core.persistence.beans.UserDerivedAttribute;
import org.syncope.core.persistence.beans.UserDerivedAttributeSchema;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.dao.UserDerivedAttributeDAO;
import org.syncope.core.persistence.dao.UserDerivedAttributeSchemaDAO;

@Transactional
public class UserDerivedAttributeDAOTest extends AbstractDAOTest {

    @Autowired
    UserDerivedAttributeDAO userDerivedAttributeDAO;
    @Autowired
    SyncopeUserDAO syncopeUserDAO;
    @Autowired
    UserDerivedAttributeSchemaDAO userDerivedAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<UserDerivedAttribute> list = userDerivedAttributeDAO.findAll();
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void testFindById() {
        UserDerivedAttribute attribute = userDerivedAttributeDAO.find(1000L);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void testSave() throws ClassNotFoundException {
        UserDerivedAttributeSchema userDerivedAttributeSchema =
                new UserDerivedAttributeSchema();
        userDerivedAttributeSchema.setName("cn2");
        userDerivedAttributeSchema.setExpression("firstname + \" \" + surname");

        userDerivedAttributeSchemaDAO.save(userDerivedAttributeSchema);

        UserDerivedAttributeSchema actualCN2Schema =
                userDerivedAttributeSchemaDAO.find("cn2");
        assertNotNull("expected save to work for CN2 schema",
                actualCN2Schema);

        SyncopeUser owner = syncopeUserDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UserDerivedAttribute derivedAttribute = new UserDerivedAttribute();
        derivedAttribute.setSchema(userDerivedAttributeSchema);
        derivedAttribute.setOwner(owner);

        derivedAttribute = userDerivedAttributeDAO.save(derivedAttribute);

        UserDerivedAttribute actual = userDerivedAttributeDAO.find(derivedAttribute.getId());
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        UserAttributeValueAsString firstnameAttribute =
                (UserAttributeValueAsString) owner.getAttribute(
                "firstname").getValues().iterator().next();
        UserAttributeValueAsString surnameAttribute =
                (UserAttributeValueAsString) owner.getAttribute(
                "surname").getValues().iterator().next();

        assertEquals("expected derived value",
                firstnameAttribute.getActualValue() + " "
                + surnameAttribute.getActualValue(),
                derivedAttribute.getValue());
    }

    @Test
    public final void testDeleteAndRelationships() {
        UserDerivedAttribute attribute = userDerivedAttributeDAO.find(1000L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        userDerivedAttributeDAO.delete(attribute.getId());

        UserDerivedAttribute actual = userDerivedAttributeDAO.find(1000L);
        assertNull("delete did not work", actual);

        UserDerivedAttributeSchema userAttributeSchema =
                userDerivedAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user derived attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
