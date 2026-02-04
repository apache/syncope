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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
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
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Test
    public void findByResourcesContaining() {
        List<Group> found = groupDAO.findByResourcesContaining(resourceDAO.findById("resource-csv").orElseThrow());
        assertEquals(2, found.size());
        assertTrue(found.contains(groupDAO.findById("0626100b-a4ba-4e00-9971-86fad52a6216").orElseThrow()));
        assertTrue(found.contains(groupDAO.findById("ba9ed509-b1f5-48ab-a334-c8530a6422dc").orElseThrow()));

        found = groupDAO.findByResourcesContaining(resourceDAO.findById("resource-testdb2").orElseThrow());
        assertTrue(found.isEmpty());
    }

    @Test
    public void uMemberships() {
        List<UMembership> memberships = groupDAO.findUMemberships(
                groupDAO.findById("37d15e4c-cdc1-460b-a591-8505c8133806").orElseThrow(), Pageable.unpaged());
        assertEquals(2, memberships.size());
        assertTrue(memberships.stream().anyMatch(m -> "3d5e91f6-305e-45f9-ad30-4897d3d43bd9".equals(m.getKey())));
        assertTrue(memberships.stream().anyMatch(m -> "d53f7657-2b22-4e10-a2cd-c3379a4d1a31".equals(m.getKey())));

        assertEquals(
                memberships.stream().map(m -> m.getLeftEnd().getKey()).collect(Collectors.toSet()),
                new HashSet<>(groupDAO.findUMembers("37d15e4c-cdc1-460b-a591-8505c8133806")));

        assertTrue(groupDAO.existsUMembership(
                "74cd8ece-715a-44a4-a736-e17b46c4e7e6", "37d15e4c-cdc1-460b-a591-8505c8133806"));
        assertFalse(groupDAO.existsUMembership(
                "74cd8ece-715a-44a4-a736-e17b46c4e7e6", "ece66293-8f31-4a84-8e8d-23da36e70846"));
        assertFalse(groupDAO.existsUMembership(
                "notfound", "ece66293-8f31-4a84-8e8d-23da36e70846"));
        assertFalse(groupDAO.existsUMembership(
                "74cd8ece-715a-44a4-a736-e17b46c4e7e6", "notfound"));

        assertEquals(2, groupDAO.countUMembers("37d15e4c-cdc1-460b-a591-8505c8133806"));
    }

    @Test
    public void saveWithTwoOwners() {
        assertThrows(InvalidEntityException.class, () -> {
            Group root = groupDAO.findByName("root").orElseThrow();

            User user = userDAO.findByUsername("rossini").orElseThrow();

            Group group = entityFactory.newEntity(Group.class);
            group.setRealm(realmDAO.getRoot());
            group.setName("error");
            group.setUserOwner(user);
            group.setGroupOwner(root);

            groupDAO.save(group);
        });
    }

    @Test
    public void findOwnedByUser() {
        Group group = groupDAO.findById("ebf97068-aa4b-4a85-9f01-680e8c4cf227").orElseThrow();

        User user = userDAO.findById("823074dc-d280-436d-a7dd-07399fae48ec").orElseThrow();

        assertEquals(user, group.getUserOwner());

        List<Group> ownedGroups = groupDAO.findOwnedByUser(user.getKey());
        assertFalse(ownedGroups.isEmpty());
        assertEquals(1, ownedGroups.size());
        assertTrue(ownedGroups.contains(group));
    }

    @Test
    public void findOwnedByGroup() {
        Group root = groupDAO.findByName("root").orElseThrow();
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("error");
        group.setGroupOwner(root);
        group = groupDAO.save(group);
        entityManager.flush();

        List<Group> owned = groupDAO.findOwnedByGroup(root.getKey());
        assertEquals(List.of(group), owned);
    }

    @Test
    public void create() {
        Group group = entityFactory.newEntity(Group.class);
        group.setRealm(realmDAO.getRoot());
        group.setName("new");

        GroupTypeExtension typeExt = entityFactory.newEntity(GroupTypeExtension.class);
        typeExt.setAnyType(anyTypeDAO.getUser());
        typeExt.add(anyTypeClassDAO.findById("csv").orElseThrow());
        typeExt.add(anyTypeClassDAO.findById("other").orElseThrow());

        group.add(typeExt);
        typeExt.setGroup(group);

        groupDAO.save(group);

        entityManager.flush();

        group = groupDAO.findByName("new").orElseThrow();
        assertEquals(1, group.getTypeExtensions().size());
        assertEquals(2, group.getTypeExtension(anyTypeDAO.getUser()).get().getAuxClasses().size());
    }

    @Test
    public void createWithInternationalCharacters() {
        Group group = entityFactory.newEntity(Group.class);
        group.setName("räksmörgås");
        group.setRealm(realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());

        groupDAO.save(group);
        entityManager.flush();
    }

    @Test
    public void delete() {
        Collection<Group> groups = userDAO.findAllGroups(userDAO.findByUsername("verdi").orElseThrow());
        assertTrue(groups.stream().anyMatch(g -> "b1f7c12d-ec83-441f-a50e-1691daaedf3b".equals(g.getKey())));
        int before = userDAO.findAllGroups(userDAO.findByUsername("verdi").orElseThrow()).size();

        groupDAO.deleteById("b1f7c12d-ec83-441f-a50e-1691daaedf3b");

        entityManager.flush();

        assertTrue(groupDAO.findById("b1f7c12d-ec83-441f-a50e-1691daaedf3b").isEmpty());
        assertEquals(before - 1, userDAO.findAllGroups(userDAO.findByUsername("verdi").orElseThrow()).size());
        assertTrue(plainSchemaDAO.findById("icon").isPresent());
    }

    @Test
    public void relationships() {
        RelationshipType groupType = entityFactory.newEntity(RelationshipType.class);
        groupType.setKey("group type");
        groupType.setLeftEndAnyType(anyTypeDAO.getGroup());
        groupType.setRightEndAnyType(anyTypeDAO.findById("PRINTER").orElseThrow());
        groupType = relationshipTypeDAO.save(groupType);

        entityManager.flush();

        Group group = groupDAO.findByName("root").orElseThrow();
        assertTrue(group.getRelationships().isEmpty());

        GRelationship newR = entityFactory.newEntity(GRelationship.class);
        newR.setType(groupType);
        newR.setLeftEnd(group);
        newR.setRightEnd(anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow());
        group.add(newR);

        groupDAO.save(group);

        entityManager.flush();

        group = groupDAO.findByName("root").orElseThrow();
        assertEquals(1, group.getRelationships().size());
        assertEquals("8559d14d-58c2-46eb-a2d4-a7d35161e8f8",
                group.getRelationships().getFirst().getRightEnd().getKey());
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

        entityManager.flush();

        group = groupDAO.findById(group.getKey()).orElseThrow();
        assertEquals("syncope's group", group.getPlainAttr("title").get().getValuesAsStrings().getFirst());
        assertEquals("syncope's group", group.getPlainAttr("originalName").get().getValuesAsStrings().getFirst());
    }
}
