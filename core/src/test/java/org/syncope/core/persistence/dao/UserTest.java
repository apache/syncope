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

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.core.rest.controller.InvalidSearchConditionException;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.types.CipherAlgorithm;

@Transactional
public class UserTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public void findAll() {
        List<SyncopeUser> list = userDAO.findAll(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()));
        assertEquals("did not get expected number of users ", 4, list.size());
    }

    @Test
    public void count() {
        Integer count = userDAO.count(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()));
        assertNotNull(count);
        assertEquals(4, count.intValue());
    }

    @Test
    public void findAllByPageAndSize() {
        Set<Long> allRoleIds =
                EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        // get first page
        List<SyncopeUser> list = userDAO.findAll(allRoleIds, 1, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page
        list = userDAO.findAll(allRoleIds, 2, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page with uncomplete set
        list = userDAO.findAll(allRoleIds, 2, 3);
        assertEquals("did not get expected number of users ", 1, list.size());

        // get unexistent page
        list = userDAO.findAll(allRoleIds, 3, 2);
        assertEquals("did not get expected number of users ", 0, list.size());
    }

    @Test
    public void findByDerAttributeValue()
            throws InvalidSearchConditionException {
        final List<SyncopeUser> list = userDAO.findByDerAttrValue(
                "cn", "Doe, John");
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test(expected = InvalidSearchConditionException.class)
    public void findByInvalidDerAttrValue()
            throws InvalidSearchConditionException {
        userDAO.findByDerAttrValue("cn", "Antonio, Maria, Rossi");
    }

    @Test(expected = InvalidSearchConditionException.class)
    public void findByInvalidDerAttrExpression()
            throws InvalidSearchConditionException {
        userDAO.findByDerAttrValue("noschema", "Antonio, Maria");
    }

    @Test
    public void findByAttributeValue() {
        final UAttrValue fullnameValue = new UAttrValue();
        fullnameValue.setStringValue("chicchiricco");

        final List<SyncopeUser> list = userDAO.findByAttrValue(
                "fullname", fullnameValue);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test
    public void findByAttributeBooleanValue() {
        final UAttrValue coolValue = new UAttrValue();
        coolValue.setBooleanValue(true);

        final List<SyncopeUser> list = userDAO.findByAttrValue(
                "cool", coolValue);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    @Test
    public void findById() {
        SyncopeUser user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);
        user = userDAO.find(3L);
        assertNotNull("did not find expected user", user);
        user = userDAO.find(5L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public void findByUsername() {
        SyncopeUser user = userDAO.find("user1");
        assertNotNull("did not find expected user", user);
        user = userDAO.find("user3");
        assertNotNull("did not find expected user", user);
        user = userDAO.find("user5");
        assertNull("found user but did not expect it", user);
    }

    @Test
    public void save() {
        SyncopeUser user = new SyncopeUser();
        user.setUsername("username");
        user.setCreationDate(new Date());

        user.setPassword("pass", CipherAlgorithm.SHA256, 0);

        Throwable t = null;
        try {
            userDAO.save(user);
        } catch (InvalidEntityException e) {
            t = e;
        }
        assertNotNull(t);

        user.setPassword("password", CipherAlgorithm.SHA256, 1);

        user.setUsername("username!");

        t = null;
        try {
            userDAO.save(user);
        } catch (InvalidEntityException e) {
            t = e;
        }
        assertNotNull(t);

        user.setUsername("username");

        SyncopeUser actual = userDAO.save(user);
        assertNotNull("expected save to work", actual);
        assertEquals(1, actual.getPasswordHistory().size());
    }

    @Test
    public void delete() {
        SyncopeUser user = userDAO.find(3L);

        userDAO.delete(user.getId());

        SyncopeUser actual = userDAO.find(3L);
        assertNull("delete did not work", actual);
    }

    @Test
    public void issue237() {
        SyncopeUser user = new SyncopeUser();
        user.setUsername("username");
        user.setCreationDate(new Date());

        user.setPassword("password", CipherAlgorithm.AES, 0);

        SyncopeUser actual = userDAO.save(user);
        assertNotNull(actual);
    }
}
