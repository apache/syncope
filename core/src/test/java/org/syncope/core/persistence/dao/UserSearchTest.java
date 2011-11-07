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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.ResourceCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.util.EntitlementUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:schedulingContext.xml",
    "classpath:workflowContext.xml"
})
@Transactional
public class UserSearchTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO searchDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public final void matches() {
        SyncopeUser user = userDAO.find(1L);
        assertNotNull(user);

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        assertFalse(searchDAO.matches(user,
                NodeCond.getLeafCond(membershipCond)));

        membershipCond.setRoleId(1L);

        assertTrue(searchDAO.matches(user,
                NodeCond.getLeafCond(membershipCond)));
    }

    @Test
    public final void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public final void searchWithNotCondition() {
        final AttributeCond fullnameLeafCond =
                new AttributeCond(AttributeCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("fabio.martelli");

        final NodeCond cond = NodeCond.getNotLeafCond(fullnameLeafCond);
        assertTrue(cond.checkValidity());

        final List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond);
        assertNotNull(users);
        assertEquals(2, users.size());

        Set<Long> ids = new HashSet<Long>(2);
        ids.add(users.get(0).getId());
        ids.add(users.get(1).getId());
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(3L));
    }

    @Test
    public final void searchByBoolean() {
        final AttributeCond coolLeafCond =
                new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        final NodeCond cond = NodeCond.getLeafCond(coolLeafCond);
        assertTrue(cond.checkValidity());

        final List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getId());
    }

    @Test
    public final void searchByPageAndSize() {
        AttributeCond fullnameLeafCond =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond),
                NodeCond.getLeafCond(membershipCond));

        assertTrue(subCond.checkValidity());

        NodeCond cond = NodeCond.getAndCond(subCond,
                NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.checkValidity());

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                cond, 1, 2);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                cond, 2, 2);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public final void searchByMembership() {
        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        List<SyncopeUser> users =
                searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                NodeCond.getLeafCond(membershipCond));
        assertNotNull(users);
        assertEquals(2, users.size());

        membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                NodeCond.getNotLeafCond(membershipCond));
        assertNotNull(users);
        assertEquals(3, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttributeCond coolLeafCond =
                new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                NodeCond.getLeafCond(coolLeafCond));
        assertNotNull(users);
        assertEquals(3, users.size());

        coolLeafCond =
                new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                NodeCond.getLeafCond(coolLeafCond));
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getNotLeafCond(ws2),
                NodeCond.getLeafCond(ws1));

        assertTrue(searchCondition.checkValidity());

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition);

        assertNotNull(users);
        assertEquals(1, users.size());
    }
}
