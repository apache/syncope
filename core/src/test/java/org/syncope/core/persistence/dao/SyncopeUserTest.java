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
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;

@Transactional
public class SyncopeUserTest extends AbstractTest {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;

    @Test
    public final void findAll() {
        List<SyncopeUser> list = syncopeUserDAO.findAll();
        assertEquals("did not get expected number of users ", 4, list.size());
    }

    @Test
    public final void findAllByPageAndSize() {
        // get first page
        List<SyncopeUser> list = syncopeUserDAO.findAll(1, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page
        list = syncopeUserDAO.findAll(2, 2);
        assertEquals("did not get expected number of users ", 2, list.size());

        // get second page with uncomplete set
        list = syncopeUserDAO.findAll(2, 3);
        assertEquals("did not get expected number of users ", 1, list.size());

        // get unexistent page
        list = syncopeUserDAO.findAll(3, 2);
        assertEquals("did not get expected number of users ", 0, list.size());
    }

    public final void findByAttribute() {
        final UserSchema schema = new UserSchema();
        schema.setName("username");

        final UserAttribute username = new UserAttribute();
        username.setSchema(schema);

        final UserAttributeValue usernameValue = new UserAttributeValue();
        usernameValue.setStringValue("chicchiricco");
        usernameValue.setAttribute(username);

        username.addValue(usernameValue);

        // get first page
        final List<SyncopeUser> list = syncopeUserDAO.findByAttributeValue(
                usernameValue);
        assertEquals("did not get expected number of users ", 1, list.size());
    }

    public final void findByAttributePageAndSize() {
        final UserSchema schema = new UserSchema();
        schema.setName("username");

        final UserAttribute username = new UserAttribute();
        username.setSchema(schema);

        final UserAttributeValue usernameValue = new UserAttributeValue();
        usernameValue.setStringValue("chicchiricco");
        usernameValue.setAttribute(username);

        username.addValue(usernameValue);

        // get first page
        List<SyncopeUser> list = syncopeUserDAO.findByAttributeValue(
                usernameValue, 1, 2);
        assertEquals("did not get expected number of users ", 1, list.size());

        // get unexistent page
        list = syncopeUserDAO.findAll(2, 2);
        assertEquals("did not get expected number of users ", 0, list.size());
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
    public final void searchWithLikeCondition() {
        AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = syncopeUserDAO.search(cond);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public final void searchWithNotCondition() {
        final AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("fabio.martelli");

        final NodeCond cond = NodeCond.getNotLeafCond(usernameLeafCond);

        assertTrue(cond.checkValidity());

        final List<SyncopeUser> users = syncopeUserDAO.search(cond);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(1L), users.get(0).getId());
    }

    @Test
    public final void searchByPageAndSize() {
        AttributeCond usernameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = syncopeUserDAO.search(cond, 1, 2);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = syncopeUserDAO.search(cond, 2, 2);
        assertNotNull(users);
        assertTrue(users.isEmpty());
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
