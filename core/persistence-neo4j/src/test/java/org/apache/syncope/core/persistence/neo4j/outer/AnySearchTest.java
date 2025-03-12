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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnySearchTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Test
    public void searchByDynMembership() {
        // 1. create role with dynamic membership
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_LIST);
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);
        role.setDynMembershipCond("cool==true");

        role = roleDAO.saveAndRefreshDynMemberships(role);
        assertNotNull(role);

        // 2. search user by this dynamic role
        RoleCond roleCond = new RoleCond();
        roleCond.setRole(role.getKey());

        List<User> users = searchDAO.search(SearchCond.of(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.getFirst().getKey());
    }

    @Test
    public void searchAsGroupOwner() {
        // 1. define rossini as member of director
        User rossini = userDAO.findByUsername("rossini").orElseThrow();

        Group group = groupDAO.findByName("director").orElseThrow();

        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(rossini);
        membership.setRightEnd(group);
        rossini.add(membership);

        userDAO.save(rossini);
        assertNotNull(rossini);

        // 2. search all users with root realm entitlements: all users are returned, including rossini
        AnyCond anyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        anyCond.setSchema("id");

        List<User> users = searchDAO.search(
                realmDAO.getRoot(), true,
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(anyCond), PageRequest.of(0, 100), AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.stream().anyMatch(user -> rossini.getKey().equals(user.getKey())));

        // 3. search all users with director owner's entitlements: only rossini is returned
        users = searchDAO.search(
                group.getRealm(), true,
                Set.of(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey())),
                SearchCond.of(anyCond), PageRequest.of(0, 100), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(rossini.getKey(), users.getFirst().getKey());
    }

    @Test
    public void searchByMembershipAttribute() {
        AnyTypeCond typeCond = new AnyTypeCond();
        typeCond.setAnyTypeKey("PRINTER");

        AttrCond attrCond = new AttrCond(AttrCond.Type.EQ);
        attrCond.setSchema("ctype");
        attrCond.setExpression("otherchildctype");
        SearchCond cond = SearchCond.and(SearchCond.of(typeCond), SearchCond.of(attrCond));

        long count = searchDAO.count(
                realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow(),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                cond,
                AnyTypeKind.ANY_OBJECT);
        assertEquals(0, count);
        List<AnyObject> results = searchDAO.search(cond, AnyTypeKind.ANY_OBJECT);
        assertTrue(results.isEmpty());

        // add any object membership and its plain attribute
        AnyObject anyObject = anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        AMembership memb = entityFactory.newEntity(AMembership.class);
        memb.setLeftEnd(anyObject);
        memb.setRightEnd(groupDAO.findByName("otherchild").orElseThrow());
        anyObject.add(memb);
        anyObject = anyObjectDAO.save(anyObject);

        PlainAttr attr = new PlainAttr();
        attr.setSchema("ctype");
        attr.add(validator, "otherchildctype");
        attr.setMembership(anyObject.getMemberships().getFirst().getKey());
        anyObject.add(attr);
        anyObjectDAO.save(anyObject);

        count = searchDAO.count(
                realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow(),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                cond,
                AnyTypeKind.ANY_OBJECT);
        assertEquals(1, count);
        results = searchDAO.search(cond, AnyTypeKind.ANY_OBJECT);
        assertEquals(1, results.size());

        assertTrue(results.stream().anyMatch(a -> "8559d14d-58c2-46eb-a2d4-a7d35161e8f8".equals(a.getKey())));
    }

    @Test
    public void issueSYNCOPE95() {
        groupDAO.findAll().forEach(group -> groupDAO.deleteById(group.getKey()));

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
    public void issueSYNCOPE1417() {
        AnyCond usernameLeafCond = new AnyCond(AnyCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("rossini");
        AttrCond idRightCond = new AttrCond(AttrCond.Type.LIKE);
        idRightCond.setSchema("fullname");
        idRightCond.setExpression("Giuseppe V%");
        SearchCond searchCondition = SearchCond.or(
                SearchCond.of(usernameLeafCond), SearchCond.of(idRightCond));

        List<Sort.Order> orderByClauses = new ArrayList<>();
        orderByClauses.add(new Sort.Order(Sort.Direction.DESC, "surname"));
        orderByClauses.add(new Sort.Order(Sort.Direction.ASC, "firstname"));

        try {
            searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE1512() {
        Group group = groupDAO.findByName("root").orElseThrow();

        // non unique
        PlainAttr title = new PlainAttr();
        title.setSchema("title");
        title.add(validator, "syncope's group");
        group.add(title);

        // unique
        PlainAttr originalName = new PlainAttr();
        originalName.setSchema("originalName");
        originalName.add(validator, "syncope's group");
        group.add(originalName);

        groupDAO.save(group);

        AttrCond titleCond = new AttrCond(AttrCond.Type.EQ);
        titleCond.setSchema("title");
        titleCond.setExpression("syncope's group");

        List<Group> matching = searchDAO.search(SearchCond.of(titleCond), AnyTypeKind.GROUP);
        assertEquals(1, matching.size());
        assertEquals(group.getKey(), matching.getFirst().getKey());

        AttrCond originalNameCond = new AttrCond(AttrCond.Type.EQ);
        originalNameCond.setSchema("originalName");
        originalNameCond.setExpression("syncope's group");

        matching = searchDAO.search(SearchCond.of(originalNameCond), AnyTypeKind.GROUP);
        assertEquals(1, matching.size());
        assertEquals(group.getKey(), matching.getFirst().getKey());
    }

    @Test
    public void issueSYNCOPE1790() {
        // 0. search by email
        AttrCond emailCond = new AttrCond(AttrCond.Type.EQ);
        emailCond.setSchema("email");
        emailCond.setExpression("verdi@syncope.org");

        SearchCond cond = SearchCond.of(emailCond);
        assertTrue(cond.isValid());

        List<User> users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("verdi", users.getFirst().getUsername());

        // 1. set rossini's email address for conditions as per SYNCOPE-1790
        User rossini = userDAO.findByUsername("rossini").orElseThrow();

        PlainAttr mail = new PlainAttr();
        mail.setSchema("email");
        mail.add(validator, "bisverdi@syncope.org");
        rossini.add(mail);

        userDAO.save(rossini);

        rossini = userDAO.findByUsername("rossini").orElseThrow();
        assertEquals(
                "bisverdi@syncope.org",
                rossini.getPlainAttr("email").map(a -> a.getValuesAsStrings().getFirst()).orElseThrow());

        // 2. search again
        users = searchDAO.search(cond, AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("verdi", users.getFirst().getUsername());
    }
}
