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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnySearchTest extends AbstractTest {

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Test
    public void anyObjectMatch() {
        AnyObject anyObject = anyObjectDAO.find(1L);
        assertNotNull(anyObject);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObjectKey(2L);
        assertTrue(searchDAO.matches(anyObject, SearchCond.getLeafCond(relationshipCond), AnyTypeKind.ANY_OBJECT));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(searchDAO.matches(anyObject, SearchCond.getLeafCond(relationshipTypeCond), AnyTypeKind.ANY_OBJECT));
    }

    @Test
    public void userMatch() {
        User user = userDAO.find(1L);
        assertNotNull(user);

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroupKey(5L);
        assertFalse(searchDAO.matches(user, SearchCond.getLeafCond(groupCond), AnyTypeKind.USER));

        groupCond.setGroupKey(1L);
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(groupCond), AnyTypeKind.USER));

        RoleCond roleCond = new RoleCond();
        roleCond.setRoleKey("Other");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(roleCond), AnyTypeKind.USER));

        user = userDAO.find(4L);
        assertNotNull(user);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObjectKey(1L);
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(relationshipCond), AnyTypeKind.USER));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(relationshipTypeCond), AnyTypeKind.USER));
    }

    @Test
    public void groupMatch() {
        Group group = groupDAO.find(1L);
        assertNotNull(group);

        AttributeCond attrCond = new AttributeCond();
        attrCond.setSchema("show");
        attrCond.setType(AttributeCond.Type.ISNOTNULL);

        assertTrue(searchDAO.matches(group, SearchCond.getLeafCond(attrCond), AnyTypeKind.GROUP));
    }

    @Test
    public void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroupKey(1L);

        AttributeCond loginDateCond = new AttributeCond(AttributeCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-26");

        SearchCond subCond = SearchCond.getAndCond(
                SearchCond.getLeafCond(fullnameLeafCond), SearchCond.getLeafCond(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAndCond(subCond, SearchCond.getLeafCond(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
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

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
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

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals(Long.valueOf(4L), users.get(0).getKey());
    }

    @Test
    public void searchByPageAndSize() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
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
                AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                cond, 2, 2, Collections.<OrderByClause>emptyList(),
                AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByGroup() {
        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroupKey(1L);

        List<User> users = searchDAO.search(SearchCond.getLeafCond(groupCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());

        groupCond = new MembershipCond();
        groupCond.setGroupKey(5L);

        users = searchDAO.search(SearchCond.getNotLeafCond(groupCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void searchByRole() {
        RoleCond roleCond = new RoleCond();
        roleCond.setRoleKey("Other");

        List<User> users = searchDAO.search(SearchCond.getLeafCond(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttributeCond coolLeafCond = new AttributeCond(AttributeCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<User> users = searchDAO.search(SearchCond.getLeafCond(coolLeafCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(SearchCond.getLeafCond(coolLeafCond), AnyTypeKind.USER);
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

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByBooleanAnyCond() {
        AttributeCond booleanCond = new AttributeCond(AnyCond.Type.EQ);
        booleanCond.setSchema("show");
        booleanCond.setExpression("true");

        List<Group> matchingGroups = searchDAO.search(SearchCond.getLeafCond(booleanCond), AnyTypeKind.GROUP);
        assertNotNull(matchingGroups);
        assertFalse(matchingGroups.isEmpty());
    }

    @Test
    public void searchByUsernameAndKey() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.LIKE);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("%ini");

        AnyCond idRightCond = new AnyCond(AnyCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("rossini", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getKey(), 0);
    }

    @Test
    public void searchByGroupNameAndKey() {
        AnyCond groupNameLeafCond = new AnyCond(AnyCond.Type.EQ);
        groupNameLeafCond.setSchema("name");
        groupNameLeafCond.setExpression("root");

        AnyCond idRightCond = new AnyCond(AnyCond.Type.LT);
        idRightCond.setSchema("key");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(groupNameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.isValid());

        List<Group> matchingGroups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertNotNull(matchingGroups);
        assertEquals(1, matchingGroups.size());
        assertEquals("root", matchingGroups.iterator().next().getName());
        assertEquals(1L, matchingGroups.iterator().next().getKey(), 0);
    }

    @Test
    public void searchByUsernameAndFullname() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");

        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");

        SearchCond searchCondition = SearchCond.getOrCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(
                searchCondition, AnyTypeKind.USER);
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchById() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(1L, users.iterator().next().getKey(), 0);

        idLeafCond = new AnyCond(AnyCond.Type.LT);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("4");

        searchCondition = SearchCond.getNotLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(IterableUtils.matchesAny(users, new Predicate<User>() {

            @Override
            public boolean evaluate(User user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void searchByType() {
        AnyTypeCond tcond = new AnyTypeCond();
        tcond.setAnyTypeName("PRINTER");

        SearchCond searchCondition = SearchCond.getLeafCond(tcond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> printers = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(printers);
        assertEquals(3, printers.size());

        tcond.setAnyTypeName("UNEXISTING");
        printers = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(printers);
        assertTrue(printers.isEmpty());
    }

    @Test
    public void searchByRelationshipType() {
        // 1. first search for printers involved in "neighborhood" relationship
        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");

        AnyTypeCond tcond = new AnyTypeCond();
        tcond.setAnyTypeName("PRINTER");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(relationshipTypeCond), SearchCond.getLeafCond(tcond));
        assertTrue(searchCondition.isValid());

        List<Any<?>> matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(matching);
        assertEquals(2, matching.size());
        assertTrue(IterableUtils.matchesAny(matching, new Predicate<Any<?>>() {

            @Override
            public boolean evaluate(final Any<?> any) {
                return any.getKey() == 1L;
            }
        }));
        assertTrue(IterableUtils.matchesAny(matching, new Predicate<Any<?>>() {

            @Override
            public boolean evaluate(final Any<?> any) {
                return any.getKey() == 2L;
            }
        }));

        // 2. search for users involved in "neighborhood" relationship
        searchCondition = SearchCond.getLeafCond(relationshipTypeCond);
        matching = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(matching);
        assertEquals(2, matching.size());
        assertTrue(IterableUtils.matchesAny(matching, new Predicate<Any<?>>() {

            @Override
            public boolean evaluate(final Any<?> any) {
                return any.getKey() == 4L;
            }
        }));
    }

    @Test
    public void userOrderBy() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
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

        List<User> users = searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
        assertEquals(
                searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.USER),
                users.size());
    }

    @Test
    public void groupOrderBy() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.LIKE);
        idLeafCond.setSchema("name");
        idLeafCond.setExpression("%r");
        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("name");

        List<Group> groups = searchDAO.search(
                searchCondition, Collections.singletonList(orderByClause), AnyTypeKind.GROUP);
        assertEquals(
                searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.GROUP),
                groups.size());
    }

    @Test
    public void assignable() {
        AssignableCond assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/even/two");
        SearchCond searchCondition = SearchCond.getLeafCond(assignableCond);
        assertTrue(searchCondition.isValid());

        List<Group> groups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertTrue(IterableUtils.matchesAny(groups, new Predicate<Group>() {

            @Override
            public boolean evaluate(final Group group) {
                return group.getKey().equals(15L);
            }
        }));
        assertFalse(IterableUtils.matchesAny(groups, new Predicate<Group>() {

            @Override
            public boolean evaluate(final Group group) {
                return group.getKey().equals(16L);
            }
        }));

        assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/odd");
        searchCondition = SearchCond.getLeafCond(assignableCond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertFalse(IterableUtils.matchesAny(anyObjects, new Predicate<AnyObject>() {

            @Override
            public boolean evaluate(final AnyObject anyObject) {
                return anyObject.getKey().equals(3L);
            }
        }));
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

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(IterableUtils.matchesAny(users, new Predicate<User>() {

            @Override
            public boolean evaluate(User user) {
                return user.getKey() == 4;
            }
        }));
    }

    @Test
    public void issue242() {
        AnyCond cond = new AnyCond(AttributeCond.Type.LIKE);
        cond.setSchema("id");
        cond.setExpression("test%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE46() {
        AnyCond cond = new AnyCond(AttributeCond.Type.LIKE);
        cond.setSchema("username");
        cond.setExpression("%ossin%");

        SearchCond searchCondition = SearchCond.getLeafCond(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void issueSYNCOPE433() {
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");

        AnyCond likeCond = new AnyCond(AttributeCond.Type.LIKE);
        likeCond.setSchema("username");
        likeCond.setExpression("%ossin%");

        SearchCond searchCond = SearchCond.getOrCond(
                SearchCond.getLeafCond(isNullCond), SearchCond.getLeafCond(likeCond));

        Integer count = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCond, AnyTypeKind.USER);
        assertNotNull(count);
        assertTrue(count > 0);
    }
}
