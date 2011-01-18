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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.user.UAttrValue;

@Transactional
public class UserTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Test
    public final void findAll() {
        List<SyncopeUser> list = userDAO.findAll();
        assertEquals("did not get expected number of users ", 4, list.size());
    }

    @Test
    public final void count() {
        Integer count = userDAO.count();
        assertNotNull(count);
        assertEquals(4, count.intValue());
    }

    @Test
    public final void findAllByPageAndSize() {
        // get first page
        List<SyncopeUser> list = userDAO.findAll(1, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page
        list = userDAO.findAll(2, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page with uncomplete set
        list = userDAO.findAll(2, 3);
        assertEquals("did not get expected number of users ", 1, list.size());

        // get unexistent page
        list = userDAO.findAll(3, 2);
        assertEquals("did not get expected number of users ", 0, list.size());
    }

    @Test
    public final void findByAttributeValue() {
        final UAttrValue usernameValue = new UAttrValue();
        usernameValue.setStringValue("chicchiricco");

        final List<SyncopeUser> list = userDAO.findByAttrValue(
                "username", usernameValue);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test
    public final void findByAttributeBooleanValue() {
        final UAttrValue coolValue = new UAttrValue();
        coolValue.setBooleanValue(true);

        final List<SyncopeUser> list = userDAO.findByAttrValue(
                "cool", coolValue);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test
    public final void findById() {
        SyncopeUser user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);
        user = userDAO.find(3L);
        assertNotNull("did not find expected user", user);
        user = userDAO.find(5L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void save() {
        SyncopeUser user = new SyncopeUser();
        user.setPassword("password");

        user = userDAO.save(user);

        SyncopeUser actual = userDAO.find(user.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void delete() {
        SyncopeUser user = userDAO.find(3L);

        userDAO.delete(user.getId());

        SyncopeUser actual = userDAO.find(3L);
        assertNull("delete did not work", actual);
    }

    @Test
    public final void getRoleResources() {
        SyncopeUser user = userDAO.find(1L);
        assertFalse(user.getInheritedTargetResources().isEmpty());
    }
}
