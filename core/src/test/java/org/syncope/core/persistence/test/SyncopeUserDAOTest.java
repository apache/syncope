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
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SyncopeUser;
import org.syncope.core.persistence.beans.UserAttributeSchema;
import org.syncope.core.persistence.beans.UserAttribute;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.dao.UserAttributeSchemaDAO;
import org.syncope.core.persistence.dao.UserAttributeDAO;

@Transactional
public class SyncopeUserDAOTest extends AbstractDAOTest {

    @Autowired
    SyncopeUserDAO syncopeUserDAO;
    @Autowired
    UserAttributeDAO userAttributeDAO;
    @Autowired
    UserAttributeSchemaDAO userAttributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<SyncopeUser> list = syncopeUserDAO.findAll();
        assertEquals("did not get expected number of users ", 3, list.size());
    }

    @Test
    public final void testFindById() {
        SyncopeUser user = syncopeUserDAO.find(1L);
        assertNotNull("did not find expected user", user);
        user = syncopeUserDAO.find(3L);
        assertNotNull("did not find expected user", user);
        user = syncopeUserDAO.find(4L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void testSave() {
        SyncopeUser user = new SyncopeUser();

        user = syncopeUserDAO.save(user);

        SyncopeUser actual = syncopeUserDAO.find(user.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        SyncopeUser user = syncopeUserDAO.find(3L);

        syncopeUserDAO.delete(user.getId());

        SyncopeUser actual = syncopeUserDAO.find(3L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void testRelationships() {
        SyncopeUser user = syncopeUserDAO.find(2L);
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

        user = syncopeUserDAO.find(2L);
        attributes = user.getAttributes();
        assertEquals("number of attributes differs",
                originalAttributesSize, attributes.size());

        UserAttributeSchema userAttributeSchema =
                userAttributeSchemaDAO.find(attributeSchemaName);
        assertNotNull("user attribute schema deleted when deleting values",
                userAttributeSchema);
    }
}
