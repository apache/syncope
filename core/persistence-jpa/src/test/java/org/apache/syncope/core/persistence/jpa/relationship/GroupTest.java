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
package org.apache.syncope.core.persistence.jpa.relationship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class GroupTest extends AbstractTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Test(expected = InvalidEntityException.class)
    public void saveWithTwoOwners() {
        Group root = groupDAO.find("root");
        assertNotNull("did not find expected group", root);

        User user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);

        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("error");
        group.setUserOwner(user);
        group.setGroupOwner(root);

        groupDAO.save(group);
    }

    @Test
    public void findByOwner() {
        Group group = groupDAO.find(6L);
        assertNotNull("did not find expected group", group);

        User user = userDAO.find(5L);
        assertNotNull("did not find expected user", user);

        assertEquals(user, group.getUserOwner());

        List<Group> ownedGroups = groupDAO.findOwnedByUser(user.getKey());
        assertFalse(ownedGroups.isEmpty());
        assertEquals(1, ownedGroups.size());
        assertTrue(ownedGroups.contains(group));
    }

    @Test
    public void delete() {
        groupDAO.delete(2L);

        groupDAO.flush();

        assertNull(groupDAO.find(2L));
        assertEquals(userDAO.findAllGroups(userDAO.find(2L)).size(), 2);
        assertNull(plainAttrDAO.find(700L, GPlainAttr.class));
        assertNull(plainAttrValueDAO.find(41L, GPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("icon"));
    }

    /**
     * Static copy of {@link org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO} method with same signature:
     * required for avoiding creating of a new transaction - good for general use case but bad for the way how
     * this test class is architected.
     */
    private Collection<Group> findDynGroupMemberships(final User user) {
        TypedQuery<Group> query = entityManager.createQuery(
                "SELECT e.group FROM " + JPAUDynGroupMembership.class.getSimpleName()
                + " e WHERE :user MEMBER OF e.users", Group.class);
        query.setParameter("user", user);

        return query.getResultList();
    }

    @Test
    public void dynMembership() {
        // 0. create user matching the condition below
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.find("/even/two"));

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(plainSchemaDAO.find("cool"));
        attribute.setOwner(user);
        attribute.add("true", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attribute);

        user = userDAO.save(user);
        Long newUserKey = user.getKey();
        assertNotNull(newUserKey);

        // 1. create group with dynamic membership
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("new");

        UDynGroupMembership dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
        dynMembership.setFIQLCond("cool==true");
        dynMembership.setGroup(group);

        group.setUDynMembership(dynMembership);

        Group actual = groupDAO.save(group);
        assertNotNull(actual);

        groupDAO.flush();

        // 2. verify that dynamic membership is there
        actual = groupDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getUDynMembership());
        assertNotNull(actual.getUDynMembership().getKey());
        assertEquals(actual, actual.getUDynMembership().getGroup());

        // 3. verify that expected users have the created group dynamically assigned
        assertEquals(2, actual.getUDynMembership().getMembers().size());
        assertEquals(new HashSet<>(Arrays.asList(4L, newUserKey)),
                CollectionUtils.collect(actual.getUDynMembership().getMembers(), new Transformer<User, Long>() {

                    @Override
                    public Long transform(final User input) {
                        return input.getKey();
                    }
                }, new HashSet<Long>()));

        user = userDAO.find(4L);
        assertNotNull(user);
        Collection<Group> dynGroupMemberships = findDynGroupMemberships(user);
        assertEquals(1, dynGroupMemberships.size());
        assertTrue(dynGroupMemberships.contains(actual.getUDynMembership().getGroup()));

        // 4. delete the new user and verify that dynamic membership was updated
        userDAO.delete(newUserKey);

        userDAO.flush();

        actual = groupDAO.find(actual.getKey());
        assertEquals(1, actual.getUDynMembership().getMembers().size());
        assertEquals(4L, actual.getUDynMembership().getMembers().get(0).getKey(), 0);

        // 5. delete group and verify that dynamic membership was also removed
        Long dynMembershipKey = actual.getUDynMembership().getKey();

        groupDAO.delete(actual);

        groupDAO.flush();

        assertNull(entityManager.find(JPAUDynGroupMembership.class, dynMembershipKey));

        dynGroupMemberships = findDynGroupMemberships(user);
        assertTrue(dynGroupMemberships.isEmpty());
    }

}
