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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnySearchTest extends AbstractTest {

    private static final String LOGIN_DATE_VALUE = "2009-05-26";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RoleDAO roleDAO;

    @BeforeEach
    public void adjustLoginDateForLocalSystem() throws ParseException {
        User rossini = userDAO.findByUsername("rossini");

        UPlainAttr loginDate = rossini.getPlainAttr("loginDate").get();
        loginDate.getValues().get(0).setDateValue(FormatUtils.parseDate(LOGIN_DATE_VALUE, "yyyy-MM-dd"));

        userDAO.save(rossini);
    }

    @Test
    public void searchWithLikeCondition() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

        AttrCond loginDateCond = new AttrCond(AttrCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression(LOGIN_DATE_VALUE);

        SearchCond subCond = SearchCond.getAnd(
                SearchCond.getLeaf(fullnameLeafCond), SearchCond.getLeaf(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAnd(subCond, SearchCond.getLeaf(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchCaseInsensitiveWithLikeCondition() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.ILIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%O%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

        AttrCond loginDateCond = new AttrCond(AttrCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression(LOGIN_DATE_VALUE);

        SearchCond subCond = SearchCond.getAnd(
                SearchCond.getLeaf(fullnameLeafCond), SearchCond.getLeaf(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAnd(subCond, SearchCond.getLeaf(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchWithNotCondition_AttrCond() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("Giuseppe Verdi");

        SearchCond cond = SearchCond.getNotLeaf(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<String> ids = users.stream().map(Entity::getKey).collect(Collectors.toSet());
        assertTrue(ids.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertTrue(ids.contains("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee"));
    }

    @Test
    public void searchWithNotCondition_AnyCond() {
        AnyCond usernameLeafCond = new AnyCond(AttrCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("verdi");

        SearchCond cond = SearchCond.getNotLeaf(usernameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        assertTrue(users.stream().noneMatch(user -> "verdi".equals(user.getUsername())));
    }

    @Test
    public void searchCaseInsensitiveWithNotCondition() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.IEQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("giuseppe verdi");

        SearchCond cond = SearchCond.getNotLeaf(fullnameLeafCond);
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
        AttrCond coolLeafCond = new AttrCond(AttrCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        SearchCond cond = SearchCond.getLeaf(coolLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.get(0).getKey());
    }

    @Test
    public void searchByRealm() {
        AnyCond anyCond = new AnyCond(AttrCond.Type.EQ);
        anyCond.setSchema("realm");
        anyCond.setExpression("c5b75db1-fce7-470f-b780-3b9934d82a9d");

        List<User> users = searchDAO.search(SearchCond.getLeaf(anyCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("rossini", users.get(0).getUsername());

        anyCond.setExpression("/even");
        users = searchDAO.search(SearchCond.getLeaf(anyCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("rossini", users.get(0).getUsername());
    }

    @Test
    public void searchByPageAndSize() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("%o%");

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("root");

        AttrCond loginDateCond = new AttrCond(AttrCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression(LOGIN_DATE_VALUE);

        SearchCond subCond = SearchCond.getAnd(
                SearchCond.getLeaf(fullnameLeafCond), SearchCond.getLeaf(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.getAnd(subCond, SearchCond.getLeaf(loginDateCond));

        assertTrue(cond.isValid());

        int count = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, cond, AnyTypeKind.USER);
        assertEquals(1, count);

        List<User> users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                cond, 1, 2, List.of(), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                cond, 2, 2, List.of(), AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByGroup() {
        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("child");

        List<User> matchingChild = searchDAO.search(SearchCond.getLeaf(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingChild);
        assertTrue(matchingChild.stream().anyMatch(user -> "verdi".equals(user.getUsername())));

        groupCond.setGroup("otherchild");

        List<User> matchingOtherChild = searchDAO.search(SearchCond.getLeaf(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingOtherChild);
        assertTrue(matchingOtherChild.stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        Set<String> union = Stream.concat(
                matchingChild.stream().map(User::getUsername),
                matchingOtherChild.stream().map(User::getUsername)).
                collect(Collectors.toSet());

        groupCond.setGroup("%child");

        List<User> matchingStar = searchDAO.search(SearchCond.getLeaf(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingStar);
        assertTrue(matchingStar.stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.stream().anyMatch(user -> "rossini".equals(user.getUsername())));
        assertEquals(union, matchingStar.stream().map(User::getUsername).collect(Collectors.toSet()));
    }

    @Test
    public void searchByRole() {
        RoleCond roleCond = new RoleCond();
        roleCond.setRole("Other");

        List<User> users = searchDAO.search(SearchCond.getLeaf(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByPrivilege() {
        PrivilegeCond privilegeCond = new PrivilegeCond();
        privilegeCond.setPrivilege("postMighty");

        List<User> users = searchDAO.search(SearchCond.getLeaf(privilegeCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttrCond coolLeafCond = new AttrCond(AttrCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<User> users = searchDAO.search(SearchCond.getLeaf(coolLeafCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(SearchCond.getLeaf(coolLeafCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceKey("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceKey("ws-target-resource-list-mappings-2");

        SearchCond searchCondition = SearchCond.getAnd(SearchCond.getNotLeaf(ws2), SearchCond.getLeaf(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByBooleanAnyCond() {
        AttrCond booleanCond = new AttrCond(AnyCond.Type.EQ);
        booleanCond.setSchema("show");
        booleanCond.setExpression("true");

        List<Group> matchingGroups = searchDAO.search(SearchCond.getLeaf(booleanCond), AnyTypeKind.GROUP);
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

        SearchCond searchCondition = SearchCond.getAnd(
                SearchCond.getLeaf(usernameLeafCond),
                SearchCond.getLeaf(idRightCond));

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
        idRightCond.setSchema("key");
        idRightCond.setExpression("37d15e4c-cdc1-460b-a591-8505c8133806");

        SearchCond searchCondition = SearchCond.getAnd(
                SearchCond.getLeaf(groupNameLeafCond),
                SearchCond.getLeaf(idRightCond));

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

        AttrCond idRightCond = new AttrCond(AttrCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");

        SearchCond searchCondition = SearchCond.getOr(
                SearchCond.getLeaf(usernameLeafCond),
                SearchCond.getLeaf(idRightCond));

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

        AttrCond idRightCond = new AttrCond(AttrCond.Type.ILIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("gIuseppe v%");

        SearchCond searchCondition = SearchCond.getOr(
                SearchCond.getLeaf(usernameLeafCond),
                SearchCond.getLeaf(idRightCond));

        List<User> matchingUsers = searchDAO.search(
                searchCondition, AnyTypeKind.USER);
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchByKey() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.EQ);
        idLeafCond.setSchema("key");
        idLeafCond.setExpression("74cd8ece-715a-44a4-a736-e17b46c4e7e6");

        SearchCond searchCondition = SearchCond.getLeaf(idLeafCond);
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

        SearchCond searchCondition = SearchCond.getLeaf(tcond);
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

        SearchCond searchCondition = SearchCond.getAnd(
                SearchCond.getLeaf(relationshipTypeCond), SearchCond.getLeaf(tcond));
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(anyObjects);
        assertEquals(2, anyObjects.size());
        assertTrue(anyObjects.stream().anyMatch(any -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(any.getKey())));
        assertTrue(anyObjects.stream().anyMatch(any -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(any.getKey())));

        // 2. search for users involved in "neighborhood" relationship
        searchCondition = SearchCond.getLeaf(relationshipTypeCond);
        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertTrue(users.stream().anyMatch(any -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(any.getKey())));
    }

    @Test
    public void searchByAnyCondDate() {
        AnyCond creationDateCond = new AnyCond(AnyCond.Type.EQ);
        creationDateCond.setSchema("creationDate");
        creationDateCond.setExpression("2021-04-15 12:45:00");

        SearchCond searchCondition = SearchCond.getLeaf(creationDateCond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(anyObjects);
        assertEquals(1, anyObjects.size());
        assertEquals("9e1d130c-d6a3-48b1-98b3-182477ed0688", anyObjects.iterator().next().getKey());
    }

    @Test
    public void searchByAttrCondDate() {
        AttrCond loginDateCond = new AttrCond(AnyCond.Type.LT);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-27");

        SearchCond searchCondition = SearchCond.getLeaf(loginDateCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", users.iterator().next().getKey());
    }

    @Test
    public void userOrderBy() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");
        AttrCond idRightCond = new AttrCond(AttrCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");
        SearchCond searchCondition = SearchCond.getOr(
                SearchCond.getLeaf(usernameLeafCond), SearchCond.getLeaf(idRightCond));

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
        SearchCond searchCondition = SearchCond.getLeaf(idLeafCond);
        assertTrue(searchCondition.isValid());

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("name");

        List<Group> groups = searchDAO.search(
                searchCondition, List.of(orderByClause), AnyTypeKind.GROUP);
        assertEquals(
                searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.GROUP),
                groups.size());
    }

    @Test
    public void assignable() {
        AssignableCond assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/even/two");
        SearchCond searchCondition = SearchCond.getLeaf(assignableCond);
        assertTrue(searchCondition.isValid());

        List<Group> groups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertTrue(groups.stream().anyMatch(group -> "additional".equals(group.getName())));
        assertFalse(groups.stream().anyMatch(group -> "fake".equals(group.getName())));

        assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/odd");
        searchCondition = SearchCond.getLeaf(assignableCond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertFalse(anyObjects.stream().
                anyMatch(anyObject -> "9e1d130c-d6a3-48b1-98b3-182477ed0688".equals(anyObject.getKey())));
    }

    @Test
    public void member() {
        MemberCond memberCond = new MemberCond();
        memberCond.setMember("1417acbe-cbf6-4277-9372-e75e04f97000");
        SearchCond searchCondition = SearchCond.getLeaf(memberCond);
        assertTrue(searchCondition.isValid());

        List<Group> groups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertEquals(2, groups.size());
        assertTrue(groups.contains(groupDAO.findByName("root")));
        assertTrue(groups.contains(groupDAO.findByName("otherchild")));
    }

    @Test
    public void asGroupOwner() {
        // prepare authentication
        Map<String, Set<String>> entForRealms = new HashMap<>();
        roleDAO.find(AuthDataAccessor.GROUP_OWNER_ROLE).getEntitlements().forEach(entitlement -> {
            Set<String> realms = Optional.ofNullable(entForRealms.get(entitlement)).orElseGet(() -> {
                Set<String> r = new HashSet<>();
                entForRealms.put(entitlement, r);
                return r;
            });

            realms.add(RealmUtils.getGroupOwnerRealm(
                    SyncopeConstants.ROOT_REALM, "37d15e4c-cdc1-460b-a591-8505c8133806"));
        });

        Set<SyncopeGrantedAuthority> authorities = new HashSet<>();
        entForRealms.forEach((key, value) -> {
            SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(key);
            authority.addRealms(value);
            authorities.add(authority);
        });

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "poorGroupOwner", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));

        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            // test count() and search()
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_SEARCH),
                    SyncopeConstants.ROOT_REALM);

            assertEquals(1, searchDAO.count(authRealms, groupDAO.getAllMatchingCond(), AnyTypeKind.GROUP));

            List<Group> groups = searchDAO.search(
                    authRealms, groupDAO.getAllMatchingCond(), 1, 10, List.of(), AnyTypeKind.GROUP);
            assertEquals(1, groups.size());
            assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", groups.get(0).getKey());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void issue202() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceKey("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceKey("ws-target-resource-list-mappings-1");

        SearchCond searchCondition =
                SearchCond.getAnd(SearchCond.getNotLeaf(ws2), SearchCond.getNotLeaf(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(user -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(user.getKey())));
    }

    @Test
    public void issue242() {
        AnyCond cond = new AnyCond(AttrCond.Type.LIKE);
        cond.setSchema("key");
        cond.setExpression("test%");

        SearchCond searchCondition = SearchCond.getLeaf(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE46() {
        AnyCond cond = new AnyCond(AttrCond.Type.LIKE);
        cond.setSchema("username");
        cond.setExpression("%ossin%");

        SearchCond searchCondition = SearchCond.getLeaf(cond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void issueSYNCOPE433() {
        AttrCond isNullCond = new AttrCond(AttrCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");

        AnyCond likeCond = new AnyCond(AttrCond.Type.LIKE);
        likeCond.setSchema("username");
        likeCond.setExpression("%ossin%");

        SearchCond searchCond = SearchCond.getOr(
                SearchCond.getLeaf(isNullCond), SearchCond.getLeaf(likeCond));

        Integer count = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCond, AnyTypeKind.USER);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void issueSYNCOPE929() {
        AttrCond rossiniCond = new AttrCond(AttrCond.Type.EQ);
        rossiniCond.setSchema("surname");
        rossiniCond.setExpression("Rossini");

        AttrCond genderCond = new AttrCond(AttrCond.Type.EQ);
        genderCond.setSchema("gender");
        genderCond.setExpression("M");

        SearchCond orCond =
                SearchCond.getOr(SearchCond.getLeaf(rossiniCond),
                        SearchCond.getLeaf(genderCond));

        AttrCond belliniCond = new AttrCond(AttrCond.Type.EQ);
        belliniCond.setSchema("surname");
        belliniCond.setExpression("Bellini");

        SearchCond searchCond =
                SearchCond.getAnd(orCond, SearchCond.getLeaf(belliniCond));

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

        entityManager().flush();

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("citizen");

        SearchCond searchCondition = SearchCond.getLeaf(groupCond);

        List<AnyObject> matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(2, matching.size());

        AnyTypeCond anyTypeCond = new AnyTypeCond();
        anyTypeCond.setAnyTypeKey(service.getKey());

        searchCondition = SearchCond.getAnd(
                SearchCond.getLeaf(groupCond), SearchCond.getLeaf(anyTypeCond));

        matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(1, matching.size());
    }

    @Test
    public void issueSYNCOPE983() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.LIKE);
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
                SearchCond.getLeaf(fullnameLeafCond),
                -1,
                -1,
                orderByClauses,
                AnyTypeKind.USER);
        assertFalse(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE1416() {
        AttrCond idLeftCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        idLeftCond.setSchema("surname");

        AttrCond idRightCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        idRightCond.setSchema("firstname");

        SearchCond searchCondition = SearchCond.getAnd(
                SearchCond.getLeaf(idLeftCond), SearchCond.getLeaf(idRightCond));

        List<OrderByClause> orderByClauses = new ArrayList<>();
        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("ctype");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        orderByClauses.add(orderByClause);

        List<User> users = searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
        assertEquals(
                searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.USER),
                users.size());

        // search by attribute with unique constraint
        AttrCond fullnameCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        fullnameCond.setSchema("fullname");

        SearchCond cond = SearchCond.getLeaf(fullnameCond);
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertEquals(5, users.size());

        fullnameCond = new AttrCond(AttrCond.Type.ISNULL);
        fullnameCond.setSchema("fullname");

        cond = SearchCond.getLeaf(fullnameCond);
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE1419() {
        AttrCond loginDateCond = new AttrCond(AttrCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression(LOGIN_DATE_VALUE);

        SearchCond cond = SearchCond.getNotLeaf(loginDateCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());
    }
}
