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
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Transactional
public class SyncopeUserDAOTest extends AbstractTest {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;

    @Test
    public final void findAll() {
        List<SyncopeUser> list = syncopeUserDAO.findAll();
        assertEquals("did not get expected number of users ", 4, list.size());
    }

    @Test
    public final void findById() {
        SyncopeUser user = syncopeUserDAO.find(1L);
        assertNotNull("did not find expected user", user);
        user = syncopeUserDAO.find(3L);
        assertNotNull("did not find expected user", user);
        user = syncopeUserDAO.find(5L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void save() {
        SyncopeUser user = new SyncopeUser();
        user.setPassword("password");

        user = syncopeUserDAO.save(user);

        SyncopeUser actual = syncopeUserDAO.find(user.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void search() {
        AttributeCond usernameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond1.setSchema("username");
        usernameLeafCond1.setExpression("%o%");

        AttributeCond usernameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond2.setSchema("username");
        usernameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond1),
                NodeCond.getLeafCond(usernameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        List<SyncopeUser> users = syncopeUserDAO.search(searchCondition);
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public final void delete() {
        SyncopeUser user = syncopeUserDAO.find(3L);

        syncopeUserDAO.delete(user.getId());

        SyncopeUser actual = syncopeUserDAO.find(3L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void getRoleResources() {
        SyncopeUser user = syncopeUserDAO.find(1L);
        assertFalse(user.getInheritedTargetResources().isEmpty());
    }
}
