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
package org.apache.syncope.core.persistence.neo4j.inner;

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
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
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
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private RoleDAO roleDAO;

    @BeforeEach
    public void adjustLoginDateForLocalSystem() throws ParseException {
        User rossini = userDAO.findByUsername("rossini").orElseThrow();

        PlainAttr loginDate = rossini.getPlainAttr("loginDate").get();
        loginDate.getValues().getFirst().setDateValue(FormatUtils.parseDate(LOGIN_DATE_VALUE, "yyyy-MM-dd"));

        userDAO.save(rossini);
    }

    @Test
    public void orOfThree() {
        AnyCond cond1 = new AnyCond(AttrCond.Type.EQ);
        cond1.setSchema("username");
        cond1.setExpression("rossini");

        AnyCond cond2 = new AnyCond(AttrCond.Type.EQ);
        cond2.setSchema("username");
        cond2.setExpression("puccini");

        AnyCond cond3 = new AnyCond(AttrCond.Type.EQ);
        cond3.setSchema("username");
        cond3.setExpression("notfound");

        SearchCond cond = SearchCond.or(List.of(
                SearchCond.of(cond1), SearchCond.of(cond2), SearchCond.of(cond3)));
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> "rossini".equals(u.getUsername())));
        assertTrue(users.stream().anyMatch(u -> "puccini".equals(u.getUsername())));
    }

    @Test
    public void searchTwoPlainSchemas() {
        AttrCond firstnameCond = new AttrCond(AttrCond.Type.EQ);
        firstnameCond.setSchema("firstname");
        firstnameCond.setExpression("Gioacchino");

        AttrCond surnameCond = new AttrCond(AttrCond.Type.EQ);
        surnameCond.setSchema("surname");
        surnameCond.setExpression("Rossini");

        SearchCond cond = SearchCond.and(SearchCond.of(firstnameCond), SearchCond.of(surnameCond));
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        surnameCond = new AttrCond(AttrCond.Type.EQ);
        surnameCond.setSchema("surname");
        surnameCond.setExpression("Verdi");

        cond = SearchCond.and(SearchCond.of(firstnameCond), SearchCond.negate(surnameCond));
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        AttrCond fullnameCond = new AttrCond(AttrCond.Type.EQ);
        fullnameCond.setSchema("fullname");
        fullnameCond.setExpression("Vincenzo Bellini");

        AttrCond userIdCond = new AttrCond(AttrCond.Type.EQ);
        userIdCond.setSchema("userId");
        userIdCond.setExpression("bellini@apache.org");

        cond = SearchCond.and(SearchCond.of(fullnameCond), SearchCond.of(userIdCond));
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        userIdCond = new AttrCond(AttrCond.Type.EQ);
        userIdCond.setSchema("userId");
        userIdCond.setExpression("rossini@apache.org");

        cond = SearchCond.and(SearchCond.of(fullnameCond), SearchCond.negate(userIdCond));
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
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

        SearchCond subCond = SearchCond.and(
                SearchCond.of(fullnameLeafCond), SearchCond.of(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.and(subCond, SearchCond.of(loginDateCond));

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

        SearchCond subCond = SearchCond.and(
                SearchCond.of(fullnameLeafCond), SearchCond.of(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.and(subCond, SearchCond.of(loginDateCond));

        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchWithNotAttrCond() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.EQ);
        fullnameLeafCond.setSchema("fullname");
        fullnameLeafCond.setExpression("Giuseppe Verdi");

        SearchCond cond = SearchCond.negate(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<String> ids = users.stream().map(User::getKey).collect(Collectors.toSet());
        assertTrue(ids.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertTrue(ids.contains("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee"));
    }

    @Test
    public void searchWithNotAnyCond() {
        AnyCond usernameLeafCond = new AnyCond(AttrCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("verdi");

        SearchCond cond = SearchCond.negate(usernameLeafCond);
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

        SearchCond cond = SearchCond.negate(fullnameLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        Set<String> ids = users.stream().map(User::getKey).collect(Collectors.toSet());
        assertTrue(ids.contains("1417acbe-cbf6-4277-9372-e75e04f97000"));
        assertTrue(ids.contains("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee"));
    }

    @Test
    public void searchByBoolean() {
        AttrCond coolLeafCond = new AttrCond(AttrCond.Type.EQ);
        coolLeafCond.setSchema("cool");
        coolLeafCond.setExpression("true");

        SearchCond cond = SearchCond.of(coolLeafCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.getFirst().getKey());
    }

    @Test
    public void searchByRealm() {
        AnyCond anyCond = new AnyCond(AttrCond.Type.EQ);
        anyCond.setSchema("realm");
        anyCond.setExpression("c5b75db1-fce7-470f-b780-3b9934d82a9d");

        List<User> users = searchDAO.search(SearchCond.of(anyCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("rossini", users.getFirst().getUsername());

        anyCond.setExpression("/even");
        users = searchDAO.search(SearchCond.of(anyCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("rossini", users.getFirst().getUsername());
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

        SearchCond subCond = SearchCond.and(
                SearchCond.of(fullnameLeafCond), SearchCond.of(groupCond));

        assertTrue(subCond.isValid());

        SearchCond cond = SearchCond.and(subCond, SearchCond.of(loginDateCond));

        assertTrue(cond.isValid());

        long count = searchDAO.count(
                realmDAO.getRoot(), true, SyncopeConstants.FULL_ADMIN_REALMS, cond, AnyTypeKind.USER);
        assertEquals(1, count);

        List<User> users = searchDAO.search(
                realmDAO.getRoot(), true, SyncopeConstants.FULL_ADMIN_REALMS, cond,
                PageRequest.of(0, 2), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());

        users = searchDAO.search(
                realmDAO.getRoot(), true, SyncopeConstants.FULL_ADMIN_REALMS, cond,
                PageRequest.of(2, 2), AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void searchByGroup() {
        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("child");

        List<User> matchingChild = searchDAO.search(SearchCond.of(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingChild);
        assertTrue(matchingChild.stream().anyMatch(user -> "verdi".equals(user.getUsername())));

        groupCond.setGroup("otherchild");

        List<User> matchingOtherChild = searchDAO.search(SearchCond.of(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingOtherChild);
        assertTrue(matchingOtherChild.stream().anyMatch(user -> "rossini".equals(user.getUsername())));

        Set<String> union = Stream.concat(
                matchingChild.stream().map(User::getUsername),
                matchingOtherChild.stream().map(User::getUsername)).
                collect(Collectors.toSet());

        groupCond.setGroup("%child");

        List<User> matchingStar = searchDAO.search(SearchCond.of(groupCond), AnyTypeKind.USER);
        assertNotNull(matchingStar);
        assertTrue(matchingStar.stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.stream().anyMatch(user -> "rossini".equals(user.getUsername())));
        assertEquals(union, matchingStar.stream().map(User::getUsername).collect(Collectors.toSet()));

        matchingStar = searchDAO.search(realmDAO.getRoot(), false, SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.of(groupCond), Pageable.unpaged(), AnyTypeKind.USER);
        assertNotNull(matchingStar);
        assertTrue(matchingStar.stream().anyMatch(user -> "verdi".equals(user.getUsername())));
        assertTrue(matchingStar.stream().noneMatch(user -> "rossini".equals(user.getUsername())));
    }

    @Test
    public void searchByRole() {
        RoleCond roleCond = new RoleCond();
        roleCond.setRole("Other");

        List<User> users = searchDAO.search(SearchCond.of(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByIsNull() {
        AttrCond coolLeafCond = new AttrCond(AttrCond.Type.ISNULL);
        coolLeafCond.setSchema("cool");

        List<User> users = searchDAO.search(SearchCond.of(coolLeafCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());

        coolLeafCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        coolLeafCond.setSchema("cool");

        users = searchDAO.search(SearchCond.of(coolLeafCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByAuxClass() {
        AuxClassCond ac = new AuxClassCond();
        ac.setAuxClass("csv");

        List<Group> groups = searchDAO.search(SearchCond.of(ac), AnyTypeKind.GROUP);
        assertNotNull(groups);
        assertEquals(2, groups.size());
    }

    @Test
    public void searchByResource() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResource("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResource("ws-target-resource-list-mappings-2");

        SearchCond searchCondition = SearchCond.and(SearchCond.negate(ws2), SearchCond.of(ws1));
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    @Test
    public void searchByBooleanAttrCond() {
        AttrCond booleanCond = new AttrCond(AnyCond.Type.EQ);
        booleanCond.setSchema("show");
        booleanCond.setExpression("true");

        List<Group> matchingGroups = searchDAO.search(SearchCond.of(booleanCond), AnyTypeKind.GROUP);
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

        SearchCond searchCondition = SearchCond.and(
                SearchCond.of(usernameLeafCond),
                SearchCond.of(idRightCond));

        List<User> matching = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(matching);
        assertEquals(1, matching.size());
        assertEquals("rossini", matching.getFirst().getUsername());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", matching.getFirst().getKey());
    }

    @Test
    public void searchByGroupNameAndKey() {
        AnyCond groupNameLeafCond = new AnyCond(AnyCond.Type.EQ);
        groupNameLeafCond.setSchema("name");
        groupNameLeafCond.setExpression("root");

        AnyCond idRightCond = new AnyCond(AnyCond.Type.EQ);
        idRightCond.setSchema("key");
        idRightCond.setExpression("37d15e4c-cdc1-460b-a591-8505c8133806");

        SearchCond searchCondition = SearchCond.and(
                SearchCond.of(groupNameLeafCond),
                SearchCond.of(idRightCond));

        assertTrue(searchCondition.isValid());

        List<Group> matching = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertNotNull(matching);
        assertEquals(1, matching.size());
        assertEquals("root", matching.getFirst().getName());
        assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", matching.getFirst().getKey());
    }

    @Test
    public void searchByUsernameAndFullname() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");

        AttrCond idRightCond = new AttrCond(AttrCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");

        SearchCond searchCondition = SearchCond.or(
                SearchCond.of(usernameLeafCond),
                SearchCond.of(idRightCond));

        List<User> matchingUsers = searchDAO.search(searchCondition, AnyTypeKind.USER);
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

        SearchCond searchCondition = SearchCond.or(
                SearchCond.of(usernameLeafCond),
                SearchCond.of(idRightCond));

        List<User> matchingUsers = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(matchingUsers);
        assertEquals(2, matchingUsers.size());
    }

    @Test
    public void searchByKey() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.EQ);
        idLeafCond.setSchema("key");
        idLeafCond.setExpression("74cd8ece-715a-44a4-a736-e17b46c4e7e6");

        SearchCond searchCondition = SearchCond.of(idLeafCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("74cd8ece-715a-44a4-a736-e17b46c4e7e6", users.getFirst().getKey());
    }

    @Test
    public void searchByType() {
        AnyTypeCond tcond = new AnyTypeCond();
        tcond.setAnyTypeKey("PRINTER");

        SearchCond searchCondition = SearchCond.of(tcond);
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
        // 1. first search for printers involved in "inclusion" relationship
        RelationshipTypeCond relationshipTypeCond = new RelationshipTypeCond();
        relationshipTypeCond.setRelationshipType("inclusion");

        AnyTypeCond tcond = new AnyTypeCond();
        tcond.setAnyTypeKey("PRINTER");

        SearchCond cond = SearchCond.and(SearchCond.of(relationshipTypeCond), SearchCond.of(tcond));
        assertTrue(cond.isValid());

        List<AnyObject> anyObjects = searchDAO.search(cond, AnyTypeKind.ANY_OBJECT);
        assertNotNull(anyObjects);
        assertEquals(2, anyObjects.size());
        assertTrue(anyObjects.stream().anyMatch(any -> "fc6dbc3a-6c07-4965-8781-921e7401a4a5".equals(any.getKey())));
        assertTrue(anyObjects.stream().anyMatch(any -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(any.getKey())));

        // 2. search for users involved in "neighborhood" relationship
        relationshipTypeCond.setRelationshipType("neighborhood");
        cond = SearchCond.of(relationshipTypeCond);
        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.getFirst().getKey());
    }

    @Test
    public void searchByAnyCondDate() {
        AnyCond creationDateCond = new AnyCond(AnyCond.Type.EQ);
        creationDateCond.setSchema("creationDate");
        creationDateCond.setExpression("2021-04-15 12:45:00");

        SearchCond searchCondition = SearchCond.of(creationDateCond);
        assertTrue(searchCondition.isValid());

        List<AnyObject> anyObjects = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertNotNull(anyObjects);
        assertEquals(1, anyObjects.size());
        assertEquals("9e1d130c-d6a3-48b1-98b3-182477ed0688", anyObjects.getFirst().getKey());
    }

    @Test
    public void searchByAttrCondDate() {
        AttrCond loginDateCond = new AttrCond(AnyCond.Type.LT);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression("2009-05-27");

        SearchCond searchCondition = SearchCond.of(loginDateCond);
        assertTrue(searchCondition.isValid());

        List<User> users = searchDAO.search(searchCondition, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", users.getFirst().getKey());
    }

    @Test
    public void userOrderBy() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");
        AttrCond idRightCond = new AttrCond(AttrCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");
        SearchCond searchCondition = SearchCond.or(
                SearchCond.of(usernameLeafCond), SearchCond.of(idRightCond));

        List<Sort.Order> orderByClauses = new ArrayList<>();
        orderByClauses.add(new Sort.Order(Sort.Direction.DESC, "username"));
        orderByClauses.add(new Sort.Order(Sort.Direction.ASC, "fullname"));
        orderByClauses.add(new Sort.Order(Sort.Direction.ASC, "status"));
        orderByClauses.add(new Sort.Order(Sort.Direction.DESC, "firstname"));

        List<User> users = searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
        assertEquals(
                searchDAO.count(
                        realmDAO.getRoot(), true,
                        SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.USER),
                users.size());
    }

    @Test
    public void groupOrderBy() {
        AnyCond idLeafCond = new AnyCond(AnyCond.Type.LIKE);
        idLeafCond.setSchema("name");
        idLeafCond.setExpression("%r");
        SearchCond searchCondition = SearchCond.of(idLeafCond);
        assertTrue(searchCondition.isValid());

        Sort.Order orderByClause = new Sort.Order(Sort.DEFAULT_DIRECTION, "name");

        List<Group> groups = searchDAO.search(
                searchCondition, List.of(orderByClause), AnyTypeKind.GROUP);
        assertEquals(
                searchDAO.count(
                        realmDAO.getRoot(), true,
                        SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.GROUP),
                groups.size());
    }

    @Test
    public void member() {
        MemberCond memberCond = new MemberCond();
        memberCond.setMember("1417acbe-cbf6-4277-9372-e75e04f97000");
        SearchCond searchCondition = SearchCond.of(memberCond);
        assertTrue(searchCondition.isValid());

        List<Group> groups = searchDAO.search(searchCondition, AnyTypeKind.GROUP);
        assertEquals(2, groups.size());
        assertTrue(groups.contains(groupDAO.findByName("root").orElseThrow()));
        assertTrue(groups.contains(groupDAO.findByName("otherchild").orElseThrow()));
    }

    @Test
    public void asGroupOwner() {
        // prepare authentication
        Map<String, Set<String>> entForRealms = new HashMap<>();
        roleDAO.findById(RoleDAO.GROUP_OWNER_ROLE).orElseThrow().getEntitlements().forEach(entitlement -> {
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

            assertEquals(
                    1,
                    searchDAO.count(
                            realmDAO.getRoot(), true, authRealms, groupDAO.getAllMatchingCond(), AnyTypeKind.GROUP));

            List<Group> groups = searchDAO.search(
                    realmDAO.getRoot(),
                    true,
                    authRealms,
                    groupDAO.getAllMatchingCond(),
                    PageRequest.of(0, 10),
                    AnyTypeKind.GROUP);
            assertEquals(1, groups.size());
            assertEquals("37d15e4c-cdc1-460b-a591-8505c8133806", groups.getFirst().getKey());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void changePwdDate() {
        AnyCond statusCond = new AnyCond(AttrCond.Type.IEQ);
        statusCond.setSchema("status");
        statusCond.setExpression("suspended");

        AnyCond changePwdDateCond = new AnyCond(AttrCond.Type.ISNULL);
        changePwdDateCond.setSchema("changePwdDate");

        SearchCond cond = SearchCond.and(SearchCond.negate(statusCond), SearchCond.of(changePwdDateCond));
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(5, users.size());
    }

    @Test
    public void issue202() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResource("ws-target-resource-2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResource("ws-target-resource-list-mappings-1");

        SearchCond searchCondition =
                SearchCond.and(SearchCond.negate(ws2), SearchCond.negate(ws1));
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

        SearchCond searchCondition = SearchCond.of(cond);
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

        SearchCond searchCondition = SearchCond.of(cond);
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

        SearchCond searchCond = SearchCond.or(
                SearchCond.of(isNullCond), SearchCond.of(likeCond));

        long count = searchDAO.count(
                realmDAO.getRoot(), true, SyncopeConstants.FULL_ADMIN_REALMS, searchCond, AnyTypeKind.USER);
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

        SearchCond orCond = SearchCond.or(SearchCond.of(rossiniCond), SearchCond.of(genderCond));

        AttrCond belliniCond = new AttrCond(AttrCond.Type.EQ);
        belliniCond.setSchema("surname");
        belliniCond.setExpression("Bellini");

        SearchCond searchCond = SearchCond.and(orCond, SearchCond.of(belliniCond));

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

        Group citizen = groupDAO.findByName("citizen").orElseThrow();
        assertNotNull(citizen);

        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("one");
        anyObject.setType(service);
        anyObject.setRealm(realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());

        AMembership membership = entityFactory.newEntity(AMembership.class);
        membership.setRightEnd(citizen);
        membership.setLeftEnd(anyObject);

        anyObject.add(membership);
        anyObjectDAO.save(anyObject);

        anyObject = anyObjectDAO.findById("fc6dbc3a-6c07-4965-8781-921e7401a4a5").orElseThrow();
        membership = entityFactory.newEntity(AMembership.class);
        membership.setRightEnd(citizen);
        membership.setLeftEnd(anyObject);
        anyObject.add(membership);
        anyObjectDAO.save(anyObject);

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("citizen");

        SearchCond searchCondition = SearchCond.of(groupCond);

        List<AnyObject> matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(2, matching.size());

        AnyTypeCond anyTypeCond = new AnyTypeCond();
        anyTypeCond.setAnyTypeKey(service.getKey());

        searchCondition = SearchCond.and(SearchCond.of(groupCond), SearchCond.of(anyTypeCond));

        matching = searchDAO.search(searchCondition, AnyTypeKind.ANY_OBJECT);
        assertEquals(1, matching.size());
    }

    @Test
    public void issueSYNCOPE983() {
        AttrCond fullnameLeafCond = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond.setSchema("surname");
        fullnameLeafCond.setExpression("%o%");

        List<Sort.Order> orderByClauses = new ArrayList<>();
        orderByClauses.add(new Sort.Order(Sort.Direction.ASC, "surname"));
        orderByClauses.add(new Sort.Order(Sort.Direction.DESC, "username"));

        List<User> users = searchDAO.search(
                realmDAO.getRoot(),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.of(fullnameLeafCond),
                Pageable.unpaged(Sort.by(orderByClauses)),
                AnyTypeKind.USER);
        assertFalse(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE1416() {
        AttrCond idLeftCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        idLeftCond.setSchema("surname");

        AttrCond idRightCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        idRightCond.setSchema("firstname");

        SearchCond searchCondition = SearchCond.and(SearchCond.of(idLeftCond), SearchCond.of(idRightCond));

        List<Sort.Order> orderByClauses = List.of(new Sort.Order(Sort.Direction.ASC, "ctype"));

        List<User> users = searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
        assertEquals(searchDAO.count(
                realmDAO.getRoot(), true, SyncopeConstants.FULL_ADMIN_REALMS, searchCondition, AnyTypeKind.USER),
                users.size());

        // search by attribute with unique constraint
        AttrCond fullnameCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        fullnameCond.setSchema("fullname");

        SearchCond cond = SearchCond.of(fullnameCond);
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertEquals(5, users.size());

        fullnameCond = new AttrCond(AttrCond.Type.ISNULL);
        fullnameCond.setSchema("fullname");

        cond = SearchCond.of(fullnameCond);
        assertTrue(cond.isValid());

        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertTrue(users.isEmpty());
    }

    @Test
    public void issueSYNCOPE1419() {
        AttrCond loginDateCond = new AttrCond(AttrCond.Type.EQ);
        loginDateCond.setSchema("loginDate");
        loginDateCond.setExpression(LOGIN_DATE_VALUE);

        SearchCond cond = SearchCond.negate(loginDateCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(4, users.size());
    }
}
