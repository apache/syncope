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
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserDerivedAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;

@Transactional
public class DerivedAttributeDAOTest extends AbstractTest {

    @Autowired
    DerivedAttributeDAO derivedAttributeDAO;
    @Autowired
    SyncopeUserDAO syncopeUserDAO;
    @Autowired
    DerivedSchemaDAO derivedAttributeSchemaDAO;
    @Autowired
    SchemaDAO attributeSchemaDAO;

    @Test
    public final void findAll() {
        List<UserDerivedAttribute> list = derivedAttributeDAO.findAll(
                UserDerivedAttribute.class);
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void findById() {
        UserDerivedAttribute attribute = derivedAttributeDAO.find(1000L,
                UserDerivedAttribute.class);
        assertNotNull("did not find expected attribute schema",
                attribute);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        UserDerivedSchema derivedAttributeSchema =
                new UserDerivedSchema();
        derivedAttributeSchema.setName("cn2");
        derivedAttributeSchema.setExpression("firstname + \" \" + surname");
        derivedAttributeSchema.addSchema(
                attributeSchemaDAO.find("firstname", UserSchema.class));
        derivedAttributeSchema.addSchema(
                attributeSchemaDAO.find("surname", UserSchema.class));

        derivedAttributeSchemaDAO.save(derivedAttributeSchema);

        UserDerivedSchema actualCN2Schema =
                derivedAttributeSchemaDAO.find("cn2", UserDerivedSchema.class);
        assertNotNull("expected save to work for CN2 schema",
                actualCN2Schema);

        SyncopeUser owner = syncopeUserDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UserDerivedAttribute derivedAttribute = new UserDerivedAttribute();
        derivedAttribute.setDerivedSchema(derivedAttributeSchema);

        derivedAttribute = derivedAttributeDAO.save(derivedAttribute);

        UserDerivedAttribute actual = derivedAttributeDAO.find(
                derivedAttribute.getId(), UserDerivedAttribute.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        UserAttributeValue firstnameAttribute =
                (UserAttributeValue) owner.getAttribute(
                "firstname").getAttributeValues().iterator().next();
        UserAttributeValue surnameAttribute =
                (UserAttributeValue) owner.getAttribute(
                "surname").getAttributeValues().iterator().next();

        assertEquals("expected derived value",
                firstnameAttribute.getValue() + " "
                + surnameAttribute.getValue(),
                derivedAttribute.getValue(owner.getAttributes()));
    }

    @Test
    public final void delete() {
        UserDerivedAttribute attribute = derivedAttributeDAO.find(1000L,
                UserDerivedAttribute.class);
        String attributeSchemaName =
                attribute.getDerivedSchema().getName();

        derivedAttributeDAO.delete(attribute.getId(),
                UserDerivedAttribute.class);

        UserDerivedAttribute actual = derivedAttributeDAO.find(1000L,
                UserDerivedAttribute.class);
        assertNull("delete did not work", actual);

        UserDerivedSchema attributeSchema =
                derivedAttributeSchemaDAO.find(attributeSchemaName,
                UserDerivedSchema.class);
        assertNotNull("user derived attribute schema deleted "
                + "when deleting values",
                attributeSchema);
    }
}
