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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALinkedAccount;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class UserTest extends AbstractTest {

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private DelegationDAO delegationDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Test
    public void delete() {
        List<UMembership> memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector"));
        assertFalse(memberships.isEmpty());

        userDAO.delete("c9b2dec2-00a7-4855-97c0-d854842b4b24");

        entityManager().flush();

        assertNull(userDAO.findByUsername("bellini"));
        assertNull(findPlainAttr(UUID.randomUUID().toString(), UPlainAttr.class));
        assertNull(findPlainAttrValue(UUID.randomUUID().toString(), UPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("loginDate"));

        memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector"));
        assertTrue(memberships.isEmpty());
    }

    @Test
    public void ships() {
        User user = userDAO.findByUsername("bellini");
        assertNotNull(user);
        assertEquals(1, user.getMemberships().size());
        assertEquals(
                "bf825fe1-7320-4a54-bd64-143b5c18ab97",
                user.getMemberships().get(0).getRightEnd().getKey());

        user.remove(user.getMemberships().get(0));

        UMembership newM = entityFactory.newEntity(UMembership.class);
        newM.setLeftEnd(user);
        newM.setRightEnd(groupDAO.find("ba9ed509-b1f5-48ab-a334-c8530a6422dc"));
        user.add(newM);

        userDAO.save(user);

        entityManager().flush();

        user = userDAO.findByUsername("bellini");
        assertEquals(1, user.getMemberships().size());
        assertEquals(
                "ba9ed509-b1f5-48ab-a334-c8530a6422dc",
                user.getMemberships().get(0).getRightEnd().getKey());
        assertEquals(1, user.getRelationships().size());
        assertEquals(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5",
                user.getRelationships().get(0).getRightEnd().getKey());

        user.getRelationships().remove(0);

        URelationship newR = entityFactory.newEntity(URelationship.class);
        newR.setType(relationshipTypeDAO.find("neighborhood"));
        newR.setLeftEnd(user);
        newR.setRightEnd(anyObjectDAO.find("8559d14d-58c2-46eb-a2d4-a7d35161e8f8"));
        user.add(newR);

        userDAO.save(user);

        entityManager().flush();

        user = userDAO.findByUsername("bellini");
        assertEquals(1, user.getRelationships().size());
        assertEquals(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8",
                user.getRelationships().get(0).getRightEnd().getKey());
    }

    @Test
    public void membershipWithAttrNotAllowed() {
        User user = userDAO.findByUsername("vivaldi");
        assertNotNull(user);
        user.getMemberships().clear();

        // add 'obscure' to user (no membership): works because 'obscure' is from 'other', default class for USER
        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        // add 'obscure' to user (via 'artDirector' membership): does not work because 'obscure' is from 'other'
        // but 'artDirector' defines no type extension
        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(user);
        membership.setRightEnd(groupDAO.findByName("artDirector"));
        user.add(membership);

        attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setMembership(membership);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue2", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        try {
            userDAO.save(user);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
        entityManager().flush();
    }

    @Test
    public void membershipWithAttr() {
        User user = userDAO.findByUsername("vivaldi");
        assertNotNull(user);
        user.getMemberships().clear();

        // add 'obscure' (no membership): works because 'obscure' is from 'other', default class for USER
        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        // add 'obscure' (via 'additional' membership): that group defines type extension with classes 'other' and 'csv'
        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setLeftEnd(user);
        membership.setRightEnd(groupDAO.findByName("additional"));
        user.add(membership);

        attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setMembership(membership);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue2", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);
        entityManager().flush();

        user = userDAO.findByUsername("vivaldi");
        assertEquals(1, user.getMemberships().size());

        UMembership newM = user.getMembership(groupDAO.findByName("additional").getKey()).get();
        assertEquals(1, user.getPlainAttrs(newM).size());

        assertNull(user.getPlainAttr("obscure").get().getMembership());
        assertEquals(2, user.getPlainAttrs("obscure").size());
        assertTrue(user.getPlainAttrs("obscure").contains(user.getPlainAttr("obscure").get()));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(plainAttr -> plainAttr.getMembership() == null));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(plainAttr -> newM.equals(plainAttr.getMembership())));
    }

    private LinkedAccount newLinkedAccount(final String connObjectKeyValue) {
        User user = userDAO.findByUsername("vivaldi");
        user.getLinkedAccounts().stream().filter(Objects::nonNull).forEach(account -> account.setOwner(null));
        user.getLinkedAccounts().clear();

        LinkedAccount account = entityFactory.newEntity(LinkedAccount.class);
        account.setOwner(user);
        user.add(account);

        account.setConnObjectKeyValue(connObjectKeyValue);
        account.setResource(resourceDAO.find("resource-ldap"));
        account.add(applicationDAO.findPrivilege("getMighty"));

        account.setUsername(UUID.randomUUID().toString());
        account.setCipherAlgorithm(CipherAlgorithm.AES);
        account.setPassword("Password123");

        AnyUtils anyUtils = anyUtilsFactory.getLinkedAccountInstance();
        LAPlainAttr attr = anyUtils.newPlainAttr();
        attr.setOwner(user);
        attr.setAccount(account);
        account.add(attr);
        attr.setSchema(plainSchemaDAO.find("obscure"));
        attr.add("testvalue", anyUtils);

        user = userDAO.save(user);
        entityManager().flush();

        assertEquals(1, user.getLinkedAccounts().size());

        return user.getLinkedAccounts().get(0);
    }

    @Test
    public void findLinkedAccount() {
        LinkedAccount account = newLinkedAccount("findLinkedAccount");
        assertNotNull(account.getKey());
        assertEquals(1, account.getPlainAttrs().size());
        assertTrue(account.getPlainAttr("obscure").isPresent());
        assertEquals(account.getOwner(), account.getPlainAttr("obscure").get().getOwner());

        assertTrue(userDAO.linkedAccountExists(account.getOwner().getKey(), account.getConnObjectKeyValue()));

        List<LinkedAccount> accounts = userDAO.findLinkedAccountsByResource(resourceDAO.find("resource-ldap"));
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.get(0));

        accounts = userDAO.findLinkedAccountsByPrivilege(applicationDAO.findPrivilege("getMighty"));
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.get(0));
    }

    @Tag("plainAttrTable")
    @Test
    public void deleteLinkedAccountUserCascade() {
        LinkedAccount account = newLinkedAccount("deleteLinkedAccountUserCascade");
        assertNotNull(account.getKey());

        LAPlainAttr plainAttr = account.getPlainAttrs().get(0);
        assertNotNull(entityManager().find(JPALAPlainAttr.class, plainAttr.getKey()));

        PlainAttrValue plainAttrValue = account.getPlainAttrs().get(0).getValues().get(0);
        assertNotNull(entityManager().find(JPALAPlainAttrValue.class, plainAttrValue.getKey()));

        LinkedAccount found = entityManager().find(JPALinkedAccount.class, account.getKey());
        assertEquals(account, found);

        userDAO.delete(account.getOwner());
        entityManager().flush();

        assertNull(entityManager().find(JPALinkedAccount.class, account.getKey()));
        assertNull(entityManager().find(JPALAPlainAttr.class, plainAttr.getKey()));
        assertNull(entityManager().find(JPALAPlainAttrValue.class, plainAttrValue.getKey()));
    }

    @Test
    public void deleteLinkedAccountResourceCascade() {
        LinkedAccount account = newLinkedAccount("deleteLinkedAccountResourceCascade");
        assertNotNull(account.getKey());

        LinkedAccount found = entityManager().find(JPALinkedAccount.class, account.getKey());
        assertEquals(account, found);

        resourceDAO.delete(account.getResource().getKey());
        entityManager().flush();

        assertNull(entityManager().find(JPALinkedAccount.class, account.getKey()));
    }

    @Test
    public void deleteCascadeOnDelegations() {
        User bellini = userDAO.findByUsername("bellini");
        User rossini = userDAO.findByUsername("rossini");

        Role reviewer = roleDAO.find("User reviewer");

        Delegation delegation = entityFactory.newEntity(Delegation.class);
        delegation.setDelegating(bellini);
        delegation.setDelegated(rossini);
        delegation.setStart(OffsetDateTime.now());
        delegation.add(reviewer);
        delegation = delegationDAO.save(delegation);

        entityManager().flush();

        delegation = delegationDAO.find(delegation.getKey());

        assertEquals(List.of(delegation), delegationDAO.findByDelegating(bellini));
        assertEquals(List.of(delegation), delegationDAO.findByDelegated(rossini));

        userDAO.delete(rossini.getKey());

        entityManager().flush();

        assertNull(delegationDAO.find(delegation.getKey()));
    }

    /**
     * Search by derived attribute.
     */
    @Test
    public void issueSYNCOPE800() {
        // create derived attribute (literal as prefix)
        DerSchema prefix = entityFactory.newEntity(DerSchema.class);
        prefix.setKey("kprefix");
        prefix.setExpression("'k' + firstname");

        derSchemaDAO.save(prefix);
        entityManager().flush();

        // create derived attribute (literal as suffix)
        DerSchema suffix = entityFactory.newEntity(DerSchema.class);
        suffix.setKey("ksuffix");
        suffix.setExpression("firstname + 'k'");

        derSchemaDAO.save(suffix);
        entityManager().flush();

        // add derived attributes to user
        User owner = userDAO.findByUsername("vivaldi");
        assertNotNull(owner);

        String firstname = owner.getPlainAttr("firstname").get().getValuesAsStrings().iterator().next();
        assertNotNull(firstname);

        // search by ksuffix derived attribute
        List<User> list = userDAO.findByDerAttrValue(derSchemaDAO.find("ksuffix"), firstname + 'k', false);
        assertEquals(1, list.size());

        // search by kprefix derived attribute
        list = userDAO.findByDerAttrValue(derSchemaDAO.find("kprefix"), 'k' + firstname, false);
        assertEquals(1, list.size());
    }
}
