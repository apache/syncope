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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.dao.JPAGroupDAO;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class GroupTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void saveWithTwoOwners() {
        assertThrows(InvalidEntityException.class, () -> {
            Group root = groupDAO.findByName("root");
            assertNotNull(root);

            User user = userDAO.findByUsername("rossini");
            assertNotNull(user);

            Group group = entityFactory.newEntity(Group.class);
            group.setRealm(realmDAO.getRoot());
            group.setName("error");
            group.setUserOwner(user);
            group.setGroupOwner(root);

            groupDAO.save(group);
        });
    }

    @Test
    public void findByOwner() {
        Group group = groupDAO.find("ebf97068-aa4b-4a85-9f01-680e8c4cf227");
        assertNotNull(group);

        User user = userDAO.find("823074dc-d280-436d-a7dd-07399fae48ec");
        assertNotNull(user);

        assertEquals(user, group.getUserOwner());

        List<Group> ownedGroups = groupDAO.findOwnedByUser(user.getKey());
        assertFalse(ownedGroups.isEmpty());
        assertEquals(1, ownedGroups.size());
        assertTrue(ownedGroups.contains(group));
    }

    @Test
    public void create() {
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("new");

        TypeExtension typeExt = entityFactory.newEntity(TypeExtension.class);
        typeExt.setAnyType(anyTypeDAO.findUser());
        typeExt.add(anyTypeClassDAO.find("csv"));
        typeExt.add(anyTypeClassDAO.find("other"));

        group.add(typeExt);
        typeExt.setGroup(group);

        groupDAO.save(group);

        entityManager().flush();

        group = groupDAO.findByName("new");
        assertNotNull(group);
        assertEquals(1, group.getTypeExtensions().size());
        assertEquals(2, group.getTypeExtension(anyTypeDAO.findUser()).get().getAuxClasses().size());
    }

    @Test
    public void createWithInternationalCharacters() {
        Group group = entityFactory.newEntity(Group.class);
        group.setName("räksmörgås");
        group.setRealm(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM));

        groupDAO.save(group);
        entityManager().flush();
    }

    @Test
    public void delete() {
        groupDAO.delete("b1f7c12d-ec83-441f-a50e-1691daaedf3b");

        entityManager().flush();

        assertNull(groupDAO.find("b1f7c12d-ec83-441f-a50e-1691daaedf3b"));
        assertEquals(userDAO.findAllGroups(userDAO.findByUsername("verdi")).size(), 2);
        assertNull(findPlainAttr("f82fc61f-8e74-4a4b-9f9e-b8a41f38aad9", GPlainAttr.class));
        assertNull(findPlainAttrValue("49f35879-2510-4f11-a901-24152f753538", GPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("icon"));
    }

    /**
     * Static copy of {@link org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO} method with same signature:
     * required for avoiding creating of a new transaction - good for general use case but bad for the way how
     * this test class is architected.
     */
    @SuppressWarnings("unchecked")
    public List<Group> findDynGroups(final User user) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.UDYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, user.getKey());

        List<Group> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(actualKey -> {
                    Group group = groupDAO.find(actualKey.toString());
                    if (group == null) {
                    } else if (!result.contains(group)) {
                        result.add(group);
                    }
                });
        return result;
    }

    @Test
    public void udynMembership() {
        // 0. create user matching the condition below
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.add(anyTypeClassDAO.find("other"));

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("cool"));
        attr.add("true", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        user = userDAO.save(user);
        String newUserKey = user.getKey();
        assertNotNull(newUserKey);

        // 1. create group with dynamic membership
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("new");

        UDynGroupMembership dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
        dynMembership.setFIQLCond("cool==true");
        dynMembership.setGroup(group);

        group.setUDynMembership(dynMembership);

        Group actual = groupDAO.saveAndRefreshDynMemberships(group);
        assertNotNull(actual);

        entityManager().flush();

        // 2. verify that dynamic membership is there
        actual = groupDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getUDynMembership());
        assertNotNull(actual.getUDynMembership().getKey());
        assertEquals(actual, actual.getUDynMembership().getGroup());

        // 3. verify that expected users have the created group dynamically assigned
        List<String> members = groupDAO.findUDynMembers(actual);
        assertEquals(2, members.size());
        assertEquals(Set.of("c9b2dec2-00a7-4855-97c0-d854842b4b24", newUserKey),
                new HashSet<>(members));

        user = userDAO.findByUsername("bellini");
        assertNotNull(user);
        Collection<Group> dynGroupMemberships = findDynGroups(user);
        assertEquals(1, dynGroupMemberships.size());
        assertTrue(dynGroupMemberships.contains(actual.getUDynMembership().getGroup()));

        // 4. delete the new user and verify that dynamic membership was updated
        userDAO.delete(newUserKey);

        entityManager().flush();

        actual = groupDAO.find(actual.getKey());
        members = groupDAO.findUDynMembers(actual);
        assertEquals(1, members.size());
        assertEquals("c9b2dec2-00a7-4855-97c0-d854842b4b24", members.get(0));

        // 5. delete group and verify that dynamic membership was also removed
        String dynMembershipKey = actual.getUDynMembership().getKey();

        groupDAO.delete(actual);

        entityManager().flush();

        assertNull(entityManager().find(JPAUDynGroupMembership.class, dynMembershipKey));

        dynGroupMemberships = findDynGroups(user);
        assertTrue(dynGroupMemberships.isEmpty());
    }

    /**
     * Static copy of {@link org.apache.syncope.core.persistence.jpa.dao.JPAAnyObjectDAO} method with same signature:
     * required for avoiding creating of a new transaction - good for general use case but bad for the way how
     * this test class is architected.
     */
    @SuppressWarnings("unchecked")
    public List<Group> findDynGroups(final AnyObject anyObject) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.ADYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, anyObject.getKey());

        List<Group> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(actualKey -> {
                    Group group = groupDAO.find(actualKey.toString());
                    if (group == null) {
                    } else if (!result.contains(group)) {
                        result.add(group);
                    }
                });
        return result;
    }

    @Test
    public void adynMembership() {
        // 0. create any object matching the condition below
        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        anyObject.setName("name");
        anyObject.setType(anyTypeDAO.find("PRINTER"));
        anyObject.setRealm(realmDAO.findByFullPath("/even/two"));

        APlainAttr attr = entityFactory.newEntity(APlainAttr.class);
        attr.setOwner(anyObject);
        attr.setSchema(plainSchemaDAO.find("model"));
        attr.add("Canon MFC8030", anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT));
        anyObject.add(attr);

        anyObject = anyObjectDAO.save(anyObject);
        String newAnyObjectKey = anyObject.getKey();
        assertNotNull(newAnyObjectKey);

        // 1. create group with dynamic membership
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("new");

        ADynGroupMembership dynMembership = entityFactory.newEntity(ADynGroupMembership.class);
        dynMembership.setAnyType(anyTypeDAO.find("PRINTER"));
        dynMembership.setFIQLCond("model==Canon MFC8030");
        dynMembership.setGroup(group);

        group.add(dynMembership);

        Group actual = groupDAO.saveAndRefreshDynMemberships(group);
        assertNotNull(actual);

        entityManager().flush();

        // 2. verify that dynamic membership is there
        actual = groupDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getADynMembership(anyTypeDAO.find("PRINTER")).get());
        assertNotNull(actual.getADynMembership(anyTypeDAO.find("PRINTER")).get().getKey());
        assertEquals(actual, actual.getADynMembership(anyTypeDAO.find("PRINTER")).get().getGroup());

        // 3. verify that expected any objects have the created group dynamically assigned
        List<String> members = groupDAO.findADynMembers(actual).stream().filter(object
                -> "PRINTER".equals(anyObjectDAO.find(object).getType().getKey())).collect(Collectors.toList());
        assertEquals(2, members.size());
        assertEquals(
                Set.of("fc6dbc3a-6c07-4965-8781-921e7401a4a5", newAnyObjectKey),
                new HashSet<>(members));

        anyObject = anyObjectDAO.find("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObject);
        Collection<Group> dynGroupMemberships = findDynGroups(anyObject);
        assertEquals(1, dynGroupMemberships.size());
        assertTrue(dynGroupMemberships.contains(actual.getADynMembership(anyTypeDAO.find("PRINTER")).get().getGroup()));

        // 4. delete the new any object and verify that dynamic membership was updated
        anyObjectDAO.delete(newAnyObjectKey);

        entityManager().flush();

        actual = groupDAO.find(actual.getKey());
        members = groupDAO.findADynMembers(actual).stream().filter(object
                -> "PRINTER".equals(anyObjectDAO.find(object).getType().getKey())).collect(Collectors.toList());
        assertEquals(1, members.size());
        assertEquals("fc6dbc3a-6c07-4965-8781-921e7401a4a5", members.get(0));

        // 5. delete group and verify that dynamic membership was also removed
        String dynMembershipKey = actual.getADynMembership(anyTypeDAO.find("PRINTER")).get().getKey();

        groupDAO.delete(actual);

        entityManager().flush();

        assertNull(entityManager().find(JPAADynGroupMembership.class, dynMembershipKey));

        dynGroupMemberships = findDynGroups(anyObject);
        assertTrue(dynGroupMemberships.isEmpty());
    }

    @Test
    public void issueSYNCOPE1512() {
        Group group = groupDAO.findByName("root");
        assertNotNull(group);

        // non unique
        GPlainAttr title = entityFactory.newEntity(GPlainAttr.class);
        title.setOwner(group);
        title.setSchema(plainSchemaDAO.find("title"));
        title.add("syncope's group", anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        group.add(title);

        // unique
        GPlainAttr originalName = entityFactory.newEntity(GPlainAttr.class);
        originalName.setOwner(group);
        originalName.setSchema(plainSchemaDAO.find("originalName"));
        originalName.add("syncope's group", anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        group.add(originalName);

        groupDAO.save(group);

        entityManager().flush();

        group = groupDAO.find(group.getKey());
        assertEquals("syncope's group", group.getPlainAttr("title").get().getValuesAsStrings().get(0));
        assertEquals("syncope's group", group.getPlainAttr("originalName").get().getValuesAsStrings().get(0));
    }
}
