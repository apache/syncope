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
import org.syncope.core.beans.SyncopeUser;
import org.syncope.core.beans.UserAttributeValue;
import org.syncope.core.beans.UserAttributeValueAsString;
import org.syncope.core.beans.UserDerivedAttributeSchema;
import org.syncope.core.beans.UserDerivedAttribute;
import org.syncope.core.dao.SyncopeUserDAO;
import org.syncope.core.dao.UserDerivedAttributeSchemaDAO;
import org.syncope.core.dao.UserDerivedAttributeDAO;

public class UserDerivedAttributeDAOTest extends AbstractDAOTest {

    SyncopeUserDAO syncopeUserDAO;
    UserDerivedAttributeSchemaDAO userDerivedAttributeSchemaDAO;

    public UserDerivedAttributeDAOTest() {
        super("userDerivedAttributeDAO");

        ApplicationContext ctx = super.getApplicationContext();

        userDerivedAttributeSchemaDAO =
                (UserDerivedAttributeSchemaDAO) ctx.getBean(
                "userDerivedAttributeSchemaDAO");
        assertNotNull(userDerivedAttributeSchemaDAO);

        syncopeUserDAO =
                (SyncopeUserDAO) ctx.getBean(
                "syncopeUserDAO");
        assertNotNull(syncopeUserDAO);
    }

    @Override
    protected UserDerivedAttributeDAO getDAO() {
        return (UserDerivedAttributeDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<UserDerivedAttribute> list = getDAO().findAll();
        assertEquals("did not get expected number of derived attributes ",
                1, list.size());
    }

    @Test
    public final void testFindById() {
        UserDerivedAttribute attribute = getDAO().find(1000L);
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

        derivedAttribute = getDAO().save(derivedAttribute);

        UserDerivedAttribute actual = getDAO().find(derivedAttribute.getId());
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
        UserDerivedAttribute attribute = getDAO().find(1000L);
        String attributeSchemaName =
                attribute.getSchema().getName();

        getDAO().delete(attribute.getId());

        UserDerivedAttribute actual = getDAO().find(1000L);
        assertNull("delete did not work", actual);

        UserDerivedAttributeSchema userAttributeSchema =
                userDerivedAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user derived attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
