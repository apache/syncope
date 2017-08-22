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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
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
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
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

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void anyObjectMatch() {
        AnyObject anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObject);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("Canon MF 8030cn");
        assertTrue(searchDAO.matches(anyObject, SearchCond.getLeafCond(relationshipCond)));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(searchDAO.matches(anyObject, SearchCond.getLeafCond(relationshipTypeCond)));
    }

    @Test
    public void userMatch() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(user);

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("secretary");
        assertFalse(searchDAO.matches(user, SearchCond.getLeafCond(groupCond)));

        groupCond.setGroup("root");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(groupCond)));

        RoleCond roleCond = new RoleCond();
        roleCond.setRole("Other");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(roleCond)));

        user = userDAO.find("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        assertNotNull(user);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(relationshipCond)));

        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipTypeKey("neighborhood");
        assertTrue(searchDAO.matches(user, SearchCond.getLeafCond(relationshipTypeCond)));
    }

    @Test
    public void groupMatch() {
        Group group = groupDAO.find("37d15e4c-cdc1-460b-a591-8505c8133806");
        assertNotNull(group);

        AttributeCond attrCond = new AttributeCond();
        attrCond.setSchema("show");
        attrCond.setType(AttributeCond.Type.ISNOTNULL);

        assertTrue(searchDAO.matches(group, SearchCond.getLeafCond(attrCond)));
    }

    @Test
    public void searchWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

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
    public void searchCaseInsensitiveWithLikeCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.ILIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%O%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

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

        Set<String> ids = users.stream().map(Entity::getKey).collect(Collectors.toSet());
        assertTrue(ids.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertTrue(ids.contains("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee"));
    }

    @Test
    public void searchCaseInsensitiveWithNotCondition() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.IEQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("giuseppe verdi");

        SearchCond cond = SearchCond.getNotLeafCond(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<String> ids = users.stream().map(Entity::getKey).collect(Collectors.toSet());
        assertTrue(ids.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertTrue(ids.contains("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee"));
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

        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.get(0).getKey());
    }

    @Test
    public void searchByPageAndSize() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

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
        groupCond.setGroup("root");

        List<User> users = searchDAO.search(SearchCond.getLeafCond(groupCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());

        groupCond = new MembershipCond();
        groupCond.setGroup("secretary");

        users = searchDAO.search(SearchCond.getNotLeafCond(groupCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void searchByRole() {
        RoleCond roleCond = new RoleCond();
        roleCond.setRole("Other");

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
        ws2.setResourceKey("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceKey("ws-target-resource-list-mappings-2");

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
        idRightCond.setSchema("id");
        idRightCond.setExpression("2");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matching = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(matching);
        assertEquals(1, matching.size());
        assertEquals("rossini", matching.iterator().next().getUsername());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matching.iterator().next().getKey());
    }

    @Test
    public void searchByGroupNameAndKey() {
        AnyCond groupNameLeafCond = new AnyCond(AnyCond.Type.EQ);
        groupNameLeafCond.setSchema("name");
        groupNameLeafCond.setExpression("root");

        AnyCond idRightCond = new AnyCond(AnyCond.Type.EQ);
        idRightCond.setSchema("id");
        idRightCond.setExpression("37d15e4c-cdc1-460b-a591-8505c8133806");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(groupNameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.isValid());

        List<Group> matching = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertNotNull(matching);
        assertEquals(1, matching.size());
        assertEquals("root", matching.iterator().next().getName());
        assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", matching.iterator().next().getKey());
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
    public void searchByUsernameAndFullnameIgnoreCase() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.IEQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("RoSsini");

        AttributeCond idRightCond = new AttributeCond(AttributeCond.Type.ILIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("gIuseppe v%");

        SearchCond searchCondition = SearchCond.getOrCond(
                SearchCond.getLeafCond(usernameLeafCond),
                SearchCond.getLeafCond(idRightCond));

        List<User> matchingUsers = searchDAO.search(
                searchCondition, AnyTypeKind.USER);
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchByKey() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.EQ);
        idLeafCond.setSchema("id");
        idLeafCond.setExpression("74cd8ece-715a-44a4-a736-e17b46c4e7e6");

        SearchCond searchCondition = SearchCond.getLeafCond(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("74cd8ece-715a-44a4-a736-e17b46c4e7e6", users.iterator().next().getKey());
    }

    @Test
    public void searchByType() {
        AnyTypeCond tcond = new AnyTypeCond();
        tcond.setAnyTypeKey("PRINTER");

        SearchCond searchCondition = SearchCond.getLeafCond(tcond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> printers = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(printers);
        assertEquals(3, printers.size());

        tcond.setAnyTypeKey("UNEXISTING");
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
        tcond.setAnyTypeKey("PRINTER");

        SearchCond searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(relationshipTypeCond), SearchCond.getLeafCond(tcond));
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(anyObjects);
        assertEquals(2, anyObjects.size());
        assertTrue(anyObjects.stream().anyMatch(any -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(any.getKey())));
        assertTrue(anyObjects.stream().anyMatch(any -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(any.getKey())));

        // 2. search for users involved in "neighborhood" relationship
        searchCondition = SearchCond.getLeafCond(relationshipTypeCond);
        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertTrue(users.stream().anyMatch(any -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(any.getKey())));
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
        assertTrue(groups.stream().anyMatch(group -> "additional".equals(group.getName())));
        assertFalse(groups.stream().anyMatch(group -> "fake".equals(group.getName())));

        assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/odd");
        searchCondition = SearchCond.getLeafCond(assignableCond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertFalse(anyObjects.stream().
                anyMatch(anyObject -> "9e1d130c-d6a3-48b1-98b3-182477ed0688".equals(anyObject.getKey())));
    }

    @Test
    public void member() {
        MemberCond memberCond = new MemberCond();
        memberCond.setMember("1417acbe-cbf6-4277-9372-e75e04f97000");
        SearchCond searchCondition = SearchCond.getLeafCond(memberCond);
        assertTrue(searchCondition.isValid());

        List<Group> groups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertEquals(2, groups.size());
        assertTrue(groups.contains(groupDAO.findByName("root")));
        assertTrue(groups.contains(groupDAO.findByName("otherchild")));
    }

    @Test
    public void issue202() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceKey("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceKey("ws-target-resource-list-mappings-1");

        SearchCond searchCondition =
                SearchCond.getAndCond(SearchCond.getNotLeafCond(ws2), SearchCond.getNotLeafCond(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
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

    @Test
    public void issueSYNCOPE929() {
        AttributeCond rossiniCond = new AttributeCond(AttributeCond.Type.EQ);
        rossiniCond.setSchema("surname");
        rossiniCond.setExpression("Rossini");

        AttributeCond genderCond = new AttributeCond(AttributeCond.Type.EQ);
        genderCond.setSchema("gender");
        genderCond.setExpression("M");

        SearchCond orCond =
                SearchCond.getOrCond(SearchCond.getLeafCond(rossiniCond),
                        SearchCond.getLeafCond(genderCond));

        AttributeCond belliniCond = new AttributeCond(AttributeCond.Type.EQ);
        belliniCond.setSchema("surname");
        belliniCond.setExpression("Bellini");

        SearchCond searchCond =
                SearchCond.getAndCond(orCond, SearchCond.getLeafCond(belliniCond));

        List<User> users = searchDAO.search(searchCond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void issueSYNCOPE980() {
        AnyType service = entityFactory.newEntity(AnyType.class);
        service.setKey("SERVICE");
        service.setKind(AnyTypeKind.ANY_OBJECT);
        service = anyTypeDAO.save(service);

        Group citizen = groupDAO.findByName("citizen");
        assertNotNull(citizen);

        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("one");
        anyObject.setType(service);
        anyObject.setRealm(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM));

        AMembership membership = entityFactory.newEntity(AMembership.class);
        membership.setRightEnd(citizen);
        membership.setLeftEnd(anyObject);

        anyObject.add(membership);
        anyObjectDAO.save(anyObject);

        anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        membership = entityFactory.newEntity(AMembership.class);
        membership.setRightEnd(citizen);
        membership.setLeftEnd(anyObject);
        anyObject.add(membership);
        anyObjectDAO.save(anyObject);

        anyObjectDAO.flush();

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("citizen");

        SearchCond searchCondition = SearchCond.getLeafCond(groupCond);

        List<AnyObject> matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(2, matching.size());

        AnyTypeCond anyTypeCond = new AnyTypeCond();
        anyTypeCond.setAnyTypeKey(service.getKey());

        searchCondition = SearchCond.getAndCond(
                SearchCond.getLeafCond(groupCond), SearchCond.getLeafCond(anyTypeCond));

        matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(1, matching.size());
    }

    @Test
    public void issueSYNCOPE983() {
        AttributeCond fullnameLeafCond = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond.setSchema("surname");
        fullnameLeafCond.setExpression("%o%");

        List<OrderByClause> orderByClauses = new ArrayList<>();
        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("surname");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        orderByClauses.add(orderByClause);
        orderByClause = new OrderByClause();
        orderByClause.setField("username");
        orderByClause.setDirection(OrderByClause.Direction.DESC);
        orderByClauses.add(orderByClause);

        List<User> users = searchDAO.search(
                SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.getLeafCond(fullnameLeafCond),
                -1,
                -1,
                orderByClauses,
                AnyTypeKind.USER);
        assertFalse(users.isEmpty());
    }
}
