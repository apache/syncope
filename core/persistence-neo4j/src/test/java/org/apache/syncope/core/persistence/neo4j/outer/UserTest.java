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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jLinkedAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
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
    private DelegationDAO delegationDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Test
    public void delete() {
        List<UMembership> memberships = groupDAO.findUMemberships(
                groupDAO.findByName("managingDirector").orElseThrow());
        assertFalse(memberships.isEmpty());

        userDAO.deleteById("c9b2dec2-00a7-4855-97c0-d854842b4b24");

        assertTrue(userDAO.findByUsername("bellini").isEmpty());
        assertTrue(plainSchemaDAO.findById("loginDate").isPresent());

        memberships = groupDAO.findUMemberships(groupDAO.findByName("managingDirector").orElseThrow());
        assertTrue(memberships.isEmpty());
    }

    @Test
    public void ships() {
        User user = userDAO.findByUsername("bellini").orElseThrow();
        assertEquals(1, user.getMemberships().size());
        assertEquals("bf825fe1-7320-4a54-bd64-143b5c18ab97", user.getMemberships().getFirst().getRightEnd().getKey());

        user.remove(user.getMemberships().getFirst());

        UMembership newM = entityFactory.newEntity(UMembership.class);
        newM.setLeftEnd(user);
        newM.setRightEnd(groupDAO.findById("ba9ed509-b1f5-48ab-a334-c8530a6422dc").orElseThrow());
        user.add(newM);

        userDAO.save(user);

        user = userDAO.findByUsername("bellini").orElseThrow();
        assertEquals(1, user.getMemberships().size());
        assertEquals("ba9ed509-b1f5-48ab-a334-c8530a6422dc", user.getMemberships().getFirst().getRightEnd().getKey());
        assertEquals(1, user.getRelationships().size());
        assertEquals("fc6dbc3a-6c07-4965-8781-921e7401a4a5", user.getRelationships().getFirst().getRightEnd().getKey());

        user.getRelationships().removeFirst();

        URelationship newR = entityFactory.newEntity(URelationship.class);
        newR.setType(relationshipTypeDAO.findById("neighborhood").orElseThrow());
        newR.setLeftEnd(user);
        newR.setRightEnd(anyObjectDAO.findById("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow());
        user.add(newR);

        userDAO.save(user);

        user = userDAO.findByUsername("bellini").orElseThrow();
        assertEquals(1, user.getRelationships().size());
        assertEquals("8559d14d-58c2-46eb-a2d4-a7d35161e8f8", user.getRelationships().getFirst().getRightEnd().getKey());
    }

    private LinkedAccount newLinkedAccount(final String connObjectKeyValue) {
        User user = userDAO.findByUsername("vivaldi").orElseThrow();
        assertTrue(user.getLinkedAccounts().isEmpty());

        LinkedAccount account = entityFactory.newEntity(LinkedAccount.class);
        account.setOwner(user);
        user.add(account);

        account.setConnObjectKeyValue(connObjectKeyValue);
        account.setResource(resourceDAO.findById("resource-ldap").orElseThrow());

        account.setUsername(UUID.randomUUID().toString());
        account.setCipherAlgorithm(CipherAlgorithm.AES);
        account.setPassword("Password123");

        PlainAttr attr = new PlainAttr();
        attr.setSchema("obscure");
        attr.add(validator, "testvalue");
        account.add(attr);

        user = userDAO.save(user);

        assertEquals(1, user.getLinkedAccounts().size());

        return user.getLinkedAccounts().getFirst();
    }

    @Test
    public void findLinkedAccount() {
        LinkedAccount account = newLinkedAccount("findLinkedAccount");
        assertNotNull(account.getKey());
        assertEquals(1, account.getPlainAttrs().size());
        assertTrue(account.getPlainAttr("obscure").isPresent());
        assertEquals("vivaldi", account.getOwner().getUsername());

        assertTrue(userDAO.linkedAccountExists(account.getOwner().getKey(), account.getConnObjectKeyValue()));

        LinkedAccount found = userDAO.findLinkedAccount(
                resourceDAO.findById("resource-ldap").orElseThrow(), "findLinkedAccount").orElseThrow();
        assertEquals(account, found);

        List<LinkedAccount> accounts = userDAO.findLinkedAccountsByResource(
                resourceDAO.findById("resource-ldap").orElseThrow());
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.getFirst());
    }

    @Test
    public void deleteLinkedAccountResourceCascade() {
        LinkedAccount account = newLinkedAccount("deleteLinkedAccountResourceCascade");
        assertNotNull(account.getKey());

        LinkedAccount found = neo4jTemplate.findById(account.getKey(), Neo4jLinkedAccount.class).orElseThrow();
        assertEquals(account, found);

        resourceDAO.deleteById(account.getResource().getKey());

        assertTrue(neo4jTemplate.findById(account.getKey(), Neo4jLinkedAccount.class).isEmpty());
    }

    @Test
    public void deleteCascadeOnDelegations() {
        User bellini = userDAO.findByUsername("bellini").orElseThrow();
        User rossini = userDAO.findByUsername("rossini").orElseThrow();

        Role reviewer = roleDAO.findById("User reviewer").orElseThrow();

        Delegation delegation = entityFactory.newEntity(Delegation.class);
        delegation.setDelegating(bellini);
        delegation.setDelegated(rossini);
        delegation.setStart(OffsetDateTime.now());
        delegation.add(reviewer);
        delegation = delegationDAO.save(delegation);

        delegation = delegationDAO.findById(delegation.getKey()).orElseThrow();

        assertEquals(List.of(delegation), delegationDAO.findByDelegating(bellini));
        assertEquals(List.of(delegation), delegationDAO.findByDelegated(rossini));

        userDAO.deleteById(rossini.getKey());

        assertTrue(delegationDAO.findById(delegation.getKey()).isEmpty());
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

        // create derived attribute (literal as suffix)
        DerSchema suffix = entityFactory.newEntity(DerSchema.class);
        suffix.setKey("ksuffix");
        suffix.setExpression("firstname + 'k'");

        derSchemaDAO.save(suffix);

        // add derived attributes to user
        User owner = userDAO.findByUsername("vivaldi").orElseThrow();

        String firstname = owner.getPlainAttr("firstname").get().getValuesAsStrings().getFirst();
        assertNotNull(firstname);

        // search by ksuffix derived attribute
        List<User> list = userDAO.findByDerAttrValue(
                derSchemaDAO.findById("ksuffix").orElseThrow().getExpression(), firstname + 'k', false);
        assertEquals(1, list.size());

        // search by kprefix derived attribute
        list = userDAO.findByDerAttrValue(
                derSchemaDAO.findById("kprefix").orElseThrow().getExpression(), 'k' + firstname, false);
        assertEquals(1, list.size());
    }
}
