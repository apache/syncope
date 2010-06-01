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
import java.util.Set;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.syncope.core.beans.SyncopeUser;
import org.syncope.core.beans.UserAttributeSchema;
import org.syncope.core.beans.UserAttribute;
import org.syncope.core.dao.SyncopeUserDAO;
import org.syncope.core.dao.UserAttributeSchemaDAO;
import org.syncope.core.dao.UserAttributeDAO;

public class SyncopeUserDAOTest extends AbstractDAOTest {

    UserAttributeDAO userAttributeDAO;
    UserAttributeSchemaDAO userAttributeSchemaDAO;

    public SyncopeUserDAOTest() {
        super("syncopeUserDAO");
        
        ApplicationContext ctx = super.getApplicationContext();
        userAttributeDAO =
                (UserAttributeDAO) ctx.getBean("userAttributeDAO");
        assertNotNull(userAttributeDAO);

        userAttributeSchemaDAO =
                (UserAttributeSchemaDAO) ctx.getBean("userAttributeSchemaDAO");
        assertNotNull(userAttributeSchemaDAO);
    }

    @Override
    protected SyncopeUserDAO getDAO() {
        return (SyncopeUserDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<SyncopeUser> list = getDAO().findAll();
        assertEquals("did not get expected number of users ", 3, list.size());
    }

    @Test
    public final void testFindById() {
        SyncopeUser user = getDAO().find(1L);
        assertNotNull("did not find expected user", user);
        user = getDAO().find(3L);
        assertNotNull("did not find expected user", user);
        user = getDAO().find(4L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void testSave() {
        SyncopeUser user = new SyncopeUser();

        user = getDAO().save(user);

        SyncopeUser actual = getDAO().find(user.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        SyncopeUser user = getDAO().find(3L);

        getDAO().delete(user.getId());

        SyncopeUser actual = getDAO().find(3L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void testRelationships() {
        SyncopeUser user = getDAO().find(2L);
        Set<UserAttribute> attributes =
                user.getAttributes();
        int originalAttributesSize = attributes.size();

        UserAttribute attribute = attributes.iterator().next();
        String attributeSchemaName =
                attribute.getSchema().getName();

        userAttributeDAO.delete(attribute.getId());
        UserAttribute actualAttribute =
                userAttributeDAO.find(attribute.getId());
        assertNull("expected delete to work", actualAttribute);

        user = getDAO().find(2L);
        attributes = user.getAttributes();
        assertEquals("number of attributes differs",
                originalAttributesSize, attributes.size());

        UserAttributeSchema userAttributeSchema =
                userAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
