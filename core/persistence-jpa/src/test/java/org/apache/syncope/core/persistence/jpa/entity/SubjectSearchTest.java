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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.GroupCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SubjectSearchTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Test
    public void userMatch() {
        User user = userDAO.find(1L);
        assertNotNull(user);

        GroupCond groupCond = new GroupCond();
        groupCond.setGroupKey(5L);
        assertFalse(searchDAO.matches(user, SearchCond.getLeafCond(groupCond), SubjectType.USER));

        groupCond.setGroupKey(1L);
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(groupCond), SubjectType.USER));

        RoleCond roleCond = new RoleCond();
        roleCond.setRoleKey(3L);
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(roleCond), SubjectType.USER));
    }

    @Test
    public void groupMatch() {
        Group group = groupDAO.find(1L);
        assertNotNull(group);

        AttributeCond attrCond = new AttributeCond();
        attrCond.setSchema("show");
        attrCond.setType(AttributeCond.Type.ISNOTNULL);

        assertTrue(searchDAO.matches(group, SearchCond.getLeafCond(attrCond), SubjectType.GROUP));
    }

    @Test
    public void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        GroupCond groupCond = new GroupCond();
        groupCond.setGroupKey(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        SearchCond subCond = SearchCond.getAndCond(SearchCond.getLeafCond(fullnameLeafCond), SearchCond.getLeafCond(
                groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAndCond(subCond, SearchCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchWithNotCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("Giuseppe Verdi");

        SearchCond cond = SearchCond.getNotLeafCond(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<Long> ids = new HashSet<>(users.size());
        for (User user : users) {
            ids.add(user.getKey());
        }
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(3L));
    }

    @Test
    public void searchByBoolean() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        SearchCond cond = SearchCond.getLeafCond(coolLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, cond, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getKey());
    }

    @Test
    public void searchByPageAndSize() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        GroupCond groupCond = new GroupCond();
        groupCond.setGroupKey(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        SearchCond subCond = SearchCond.getAndCond(
                SearchCond.getLeafCond(fullnameLeafCond), SearchCond.getLeafCond(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAndCond(subCond, SearchCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                cond, 1, 2, Collections.<OrderByClause>emptyList(),
                SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                cond, 2, 2, Collections.<OrderByClause>emptyList(),
                SubjectType.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByGroup() {
        GroupCond groupCond = new GroupCond();
        groupCond.setGroupKey(1L);

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getLeafCond(groupCond), SubjectType.USER);
        assertNotNull(users);
        assertEquals(2, users.size());

        groupCond = new GroupCond();
        groupCond.setGroupKey(5L);

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getNotLeafCond(groupCond), SubjectType.USER);
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void searchByRole() {
        RoleCond roleCond = new RoleCond();
        roleCond.setRoleKey(3L);

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getLeafCond(roleCond), SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<User> users = searchDAO.search(
                SyncopeConstants.FULL_ADMIN_REALMS, SearchCond.getLeafCond(coolLeafCond), SubjectType.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getLeafCond(coolLeafCond), SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        SearchCond searchCondition = SearchCond.getAndCond(SearchCond.getNotLeafCond(ws2), SearchCond.getLeafCond(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByBooleanSubjectCond() {
        AttributeCond booleanCond = new AttributeCond(SubjectCond.Type.EQ);
        booleanCond.setSchema("show");
        booleanCond.setExpression("true");

        List<Group> matchingGroups = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getLeafCond(booleanCond), SubjectType.GROUP);
        assertNotNull(matchingGroups);
        assertFalse(matchingGroups.isEmpty());
    }

    @Test
    public void searchByUsernameAndKey() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%ini");

        SubjectCond idRightCond = new SubjectCond(SubjectCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, SubjectType.USER);

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("rossini", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getKey(), 0);
    }

    @Test
    public void searchByGroupNameAndKey() {
        SubjectCond groupNameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        groupNameLeafCond.setSchema("name");
        groupNameLeafCond.setExpression("root");

        SubjectCond idRightCond = new SubjectCond(SubjectCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(groupNameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.isValid());

        List<Group> matchingGroups = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, SubjectType.GROUP);

        assertNotNull(matchingGroups);
        assertEquals(1, matchingGroups.size());
        assertEquals("root", matchingGroups.iterator().next().getName());
        assertEquals(1L, matchingGroups.iterator().next().getKey(), 0);
    }

    @Test
    public void searchByUsernameAndFullname() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");

        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");

        SearchCond searchCondition = SearchCond.getOrCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, SubjectType.USER);
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchById() {
        SubjectCond idLeafCond = new SubjectCond(SubjectCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(1L, users.iterator().next().getKey(), 0);

        idLeafCond = new SubjectCond(SubjectCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("4");

        searchCondition = SearchCond.getNotLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(CollectionUtils.exists(users, new Predicate<User>() {

            @Override
            public boolean evaluate(User user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void userOrderBy() {
        SubjectCond usernameLeafCond = new SubjectCond(SubjectCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");
        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");
        SearchCond searchCondition = SearchCond.getOrCond(
                SearchCond.getLeafCond(usernameLeafCond), SearchCond.getLeafCond(idRightCond));

        List<OrderByClause> orderByClauses = new ArrayList<>();
        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("username");
        orderByClause.setDirection(OrderByClause.Direction.DESC);
        orderByClauses.add(orderByClause);
        orderByClause = new OrderByClause();
        orderByClause.setField("fullname");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        orderByClauses.add(orderByClause);

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, orderByClauses, SubjectType.USER);
        assertEquals(searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER),
                users.size());
    }

    @Test
    public void groupOrderBy() {
        SubjectCond idLeafCond = new SubjectCond(SubjectCond.Type.LIKE);
        idLeafCond.setSchema("name");
        idLeafCond.setExpression("%r");
        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("name");

        List<Group> groups = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, Collections.singletonList(orderByClause), SubjectType.GROUP);
        assertEquals(searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS,
                searchCondition, SubjectType.GROUP),
                groups.size());
    }

    @Test
    public void issue202() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-1");

        SearchCond searchCondition =
                SearchCond.getAndCond(SearchCond.getNotLeafCond(ws2), SearchCond.getNotLeafCond(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(CollectionUtils.exists(users, new Predicate<User>() {

            @Override
            public boolean evaluate(User user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void issue242() {
        SubjectCond cond = new SubjectCond(AttributeCond.Type.LIKE);
        cond.setSchema("id");
        cond.setExpression("test%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE46() {
        SubjectCond cond = new SubjectCond(AttributeCond.Type.LIKE);
        cond.setSchema("username");
        cond.setExpression("%ossin%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, SubjectType.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void issueSYNCOPE433() {
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");

        SubjectCond likeCond = new SubjectCond(AttributeCond.Type.LIKE);
        likeCond.setSchema("username");
        likeCond.setExpression("%ossin%");

        SearchCond searchCond = SearchCond.getOrCond(
                SearchCond.getLeafCond(isNullCond), SearchCond.getLeafCond(likeCond));

        Integer count = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCond, SubjectType.USER);
        assertNotNull(count);
        assertTrue(count > 0);
    }
}
