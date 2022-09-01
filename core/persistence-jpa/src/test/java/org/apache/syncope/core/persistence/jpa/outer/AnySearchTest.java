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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.attrvalue.validation.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnySearchTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Test
    public void searchByDynMembership() {
        // 1. create role with dynamic membership
        Role role = entityFactory.newEntity(Role.class);
        role.setKey("new");
        role.add(realmDAO.getRoot());
        role.add(realmDAO.findByFullPath("/even/two"));
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_LIST);
        role.getEntitlements().add(IdRepoEntitlement.AUDIT_SET);

        DynRoleMembership dynMembership = entityFactory.newEntity(DynRoleMembership.class);
        dynMembership.setFIQLCond("cool==true");
        dynMembership.setRole(role);

        role.setDynMembership(dynMembership);

        role = roleDAO.saveAndRefreshDynMemberships(role);
        assertNotNull(role);

        entityManager().flush();

        // 2. search user by this dynamic role
        RoleCond roleCond = new RoleCond();
        roleCond.setRole(role.getKey());

        List<User> users = searchDAO.search(SearchCond.getLeaf(roleCond), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", users.get(0).getKey());
    }

    @Test
    public void searchAsGroupOwner() {
        // 1. define rossini as member of director
        User rossini = userDAO.findByUsername("rossini");
        assertNotNull(rossini);

        Group group = groupDAO.findByName("director");
        assertNotNull(group);

        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(rossini);
        membership.setRightEnd(group);
        rossini.add(membership);

        userDAO.save(rossini);
        assertNotNull(rossini);

        entityManager().flush();

        // 2. search all users with root realm entitlements: all users are returned, including rossini
        AnyCond anyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        anyCond.setSchema("id");

        List<User> users = searchDAO.search(
                realmDAO.getRoot(), true,
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.getLeaf(anyCond), 1, 100, Collections.emptyList(), AnyTypeKind.USER);
        assertNotNull(users);
        assertTrue(users.stream().anyMatch(user -> rossini.getKey().equals(user.getKey())));

        // 3. search all users with director owner's entitlements: only rossini is returned
        users = searchDAO.search(
                group.getRealm(), true,
                Set.of(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey())),
                SearchCond.getLeaf(anyCond), 1, 100, Collections.emptyList(), AnyTypeKind.USER);
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(rossini.getKey(), users.get(0).getKey());
    }

    @Test
    public void issueSYNCOPE95() {
        groupDAO.findAll(1, 100).forEach(group -> groupDAO.delete(group.getKey()));
        entityManager().flush();

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
    public void issueSYNCOPE1417() {
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
        orderByClause.setField("surname");
        orderByClause.setDirection(OrderByClause.Direction.DESC);
        orderByClauses.add(orderByClause);
        orderByClause = new OrderByClause();
        orderByClause.setField("firstname");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        orderByClauses.add(orderByClause);

        try {
            searchDAO.search(searchCondition, orderByClauses, AnyTypeKind.USER);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE1512() {
        Group group = groupDAO.findByName("root");
        assertNotNull(group);

        // non unique
        GPlainAttr title = entityFactory.newEntity(GPlainAttr.class);
        title.setOwner(group);
        title.setSchema(plainSchemaDAO.find("title"));
        title.add(validator, "syncope's group", anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        group.add(title);

        // unique
        GPlainAttr originalName = entityFactory.newEntity(GPlainAttr.class);
        originalName.setOwner(group);
        originalName.setSchema(plainSchemaDAO.find("originalName"));
        originalName.add(validator, "syncope's group", anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        group.add(originalName);

        groupDAO.save(group);

        entityManager().flush();

        AttrCond titleCond = new AttrCond(AttrCond.Type.EQ);
        titleCond.setSchema("title");
        titleCond.setExpression("syncope's group");

        List<Group> matching = searchDAO.search(SearchCond.getLeaf(titleCond), AnyTypeKind.GROUP);
        assertEquals(1, matching.size());
        assertEquals(group.getKey(), matching.get(0).getKey());

        AttrCond originalNameCond = new AttrCond(AttrCond.Type.EQ);
        originalNameCond.setSchema("originalName");
        originalNameCond.setExpression("syncope's group");

        matching = searchDAO.search(SearchCond.getLeaf(originalNameCond), AnyTypeKind.GROUP);
        assertEquals(1, matching.size());
        assertEquals(group.getKey(), matching.get(0).getKey());
    }
}
