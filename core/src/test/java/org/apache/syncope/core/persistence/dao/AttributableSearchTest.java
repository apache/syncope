/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.search.AttributableCond;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.MembershipCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.search.ResourceCond;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:persistenceTestEnv.xml"})
@Transactional
public class AttributableSearchTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private AttributableSearchDAO searchDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public void userMatch() {
        SyncopeUser user = userDAO.find(1L);
        assertNotNull(user);

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        assertFalse(searchDAO.matches(user, NodeCond.getLeafCond(membershipCond),
                AttributableUtil.getInstance(AttributableType.USER)));

        membershipCond.setRoleId(1L);

        assertTrue(searchDAO.matches(user, NodeCond.getLeafCond(membershipCond),
                AttributableUtil.getInstance(AttributableType.USER)));
    }

    @Test
    public void roleMatch() {
        SyncopeRole role = roleDAO.find(1L);
        assertNotNull(role);

        AttributeCond attrCond = new AttributeCond();
        attrCond.setSchema("show");
        attrCond.setType(AttributeCond.Type.ISNOTNULL);

        assertTrue(searchDAO.matches(role, NodeCond.getLeafCond(attrCond),
                AttributableUtil.getInstance(AttributableType.ROLE)));
    }

    @Test
    public void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(NodeCond.getLeafCond(fullnameLeafCond), NodeCond.getLeafCond(
                membershipCond));

        assertTrue(subCond.isValid());

        NodeCond cond = NodeCond.getAndCond(subCond, NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond,
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchWithNotCondition() {
        final AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("fabio.martelli");

        final NodeCond cond = NodeCond.getNotLeafCond(fullnameLeafCond);
        assertTrue(cond.isValid());

        final List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond,
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(2, users.size());

        Set<Long> ids = new HashSet<Long>(2);
        ids.add(users.get(0).getId());
        ids.add(users.get(1).getId());
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(3L));
    }

    @Test
    public void searchByBoolean() {
        final AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        final NodeCond cond = NodeCond.getLeafCond(coolLeafCond);
        assertTrue(cond.isValid());

        final List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond,
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getId());
    }

    @Test
    public void searchByPageAndSize() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        NodeCond subCond = NodeCond.getAndCond(NodeCond.getLeafCond(fullnameLeafCond), NodeCond.getLeafCond(
                membershipCond));

        assertTrue(subCond.isValid());

        NodeCond cond = NodeCond.getAndCond(subCond, NodeCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond, 1, 2,
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), cond, 2, 2,
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByMembership() {
        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(1L);

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), NodeCond.getLeafCond(membershipCond),
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(2, users.size());

        membershipCond = new MembershipCond();
        membershipCond.setRoleId(5L);

        users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), NodeCond.getNotLeafCond(membershipCond),
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), NodeCond.getLeafCond(coolLeafCond),
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), NodeCond.getLeafCond(coolLeafCond),
                AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getNotLeafCond(ws2), NodeCond.getLeafCond(ws1));

        assertTrue(searchCondition.isValid());

        List<SyncopeUser> users = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCondition,
                AttributableUtil.getInstance(AttributableType.USER));

        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByUsernameAndId() {
        final AttributableCond usernameLeafCond = new AttributableCond(AttributableCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("user1");

        final AttributableCond idRightCond = new AttributableCond(AttributableCond.Type.LT);
        idRightCond.setSchema("id");
        idRightCond.setExpression("2");

        final NodeCond searchCondition = NodeCond.getOrCond(NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(idRightCond));

        final List<SyncopeUser> matchingUsers = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("user1", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getId().longValue());
    }

    @Test
    public void searchByRolenameAndId() {
        final AttributableCond rolenameLeafCond = new AttributableCond(AttributableCond.Type.EQ);
        rolenameLeafCond.setSchema("name");
        rolenameLeafCond.setExpression("root");

        final AttributableCond idRightCond = new AttributableCond(AttributableCond.Type.LT);
        idRightCond.setSchema("id");
        idRightCond.setExpression("2");

        final NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getLeafCond(rolenameLeafCond),
                NodeCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.isValid());

        final List<SyncopeRole> matchingRoles = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition, AttributableUtil.getInstance(AttributableType.ROLE));

        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.size());
        assertEquals("root", matchingRoles.iterator().next().getName());
        assertEquals(1L, matchingRoles.iterator().next().getId().longValue());
    }

    @Test
    public void searchByUsernameAndFullname() {
        final AttributableCond usernameLeafCond = new AttributableCond(AttributableCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("user1");

        final AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("fabio.mart%");

        final NodeCond searchCondition = NodeCond.getOrCond(NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(idRightCond));

        final List<SyncopeUser> matchingUsers =
                searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCondition,
                AttributableUtil.getInstance(AttributableType.USER));

        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchById() {
        AttributableCond idLeafCond = new AttributableCond(AttributableCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("2");

        NodeCond searchCondition = NodeCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<SyncopeUser> matchingUsers =
                searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCondition,
                AttributableUtil.getInstance(AttributableType.USER));

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals(1L, matchingUsers.iterator().next().getId().longValue());

        idLeafCond = new AttributableCond(AttributableCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("4");

        searchCondition = NodeCond.getNotLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        matchingUsers = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()), searchCondition,
                AttributableUtil.getInstance(AttributableType.USER));

        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
        assertEquals(4L, matchingUsers.iterator().next().getId().longValue());
    }

    @Test
    public void issue202() {
        final ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        final ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-1");

        final NodeCond searchCondition =
                NodeCond.getAndCond(NodeCond.getNotLeafCond(ws2), NodeCond.getNotLeafCond(ws1));
        assertTrue(searchCondition.isValid());

        final List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals(4L, users.iterator().next().getId().longValue());
    }

    @Test
    public void issue242() {
        final AttributableCond cond = new AttributableCond(AttributeCond.Type.LIKE);
        cond.setSchema("id");
        cond.setExpression("test%");

        final NodeCond searchCondition = NodeCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        final List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE46() {
        final AttributableCond cond = new AttributableCond(AttributeCond.Type.LIKE);
        cond.setSchema("username");
        cond.setExpression("%user%");

        final NodeCond searchCondition = NodeCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        final List<SyncopeUser> users = searchDAO.search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
        assertNotNull(users);
        assertEquals(5, users.size());
    }
}
