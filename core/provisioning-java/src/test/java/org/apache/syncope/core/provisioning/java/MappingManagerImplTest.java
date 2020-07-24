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
package org.apache.syncope.core.provisioning.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class MappingManagerImplTest extends AbstractTest {

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    public void prepareAttrsForUser() {
        User bellini = userDAO.findByUsername("bellini");
        ExternalResource ldap = resourceDAO.find("resource-ldap");
        Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).get();

        assertNotEquals(CipherAlgorithm.AES, bellini.getCipherAlgorithm());

        // 1. with clear-text password
        Pair<String, Set<Attribute>> attrs = mappingManager.prepareAttrsFromAny(
                bellini,
                "Password123",
                true,
                Boolean.TRUE,
                provision);
        assertEquals("bellini", attrs.getLeft());
        assertEquals(
                "uid=bellini,ou=people,o=isp",
                AttributeUtil.getNameFromAttributes(attrs.getRight()).getNameValue());
        assertEquals("Password123", SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs.getRight())));

        // 2. with changePwd == false
        attrs = mappingManager.prepareAttrsFromAny(
                bellini,
                "Password123",
                false,
                Boolean.TRUE,
                provision);
        assertNull(AttributeUtil.getPasswordValue(attrs.getRight()));

        // 3. with no clear-text password but random password generation enabled
        ldap.setRandomPwdIfNotProvided(true);
        ldap = resourceDAO.save(ldap);
        entityManager().flush();

        String encPassword = bellini.getPassword();
        attrs = mappingManager.prepareAttrsFromAny(
                bellini,
                null,
                true,
                Boolean.TRUE,
                provision);
        assertNotEquals(encPassword, SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs.getRight())));

        // 4. with no clear-text password and random password generation disabled
        ldap.setRandomPwdIfNotProvided(false);
        resourceDAO.save(ldap);
        entityManager().flush();

        attrs = mappingManager.prepareAttrsFromAny(
                bellini,
                null,
                true,
                Boolean.TRUE,
                provision);
        assertNull(AttributeUtil.getPasswordValue(attrs.getRight()));

        // 5. with no clear-text password, random password generation disabled but AES
        bellini.setPassword("newPassword123", CipherAlgorithm.AES);
        userDAO.save(bellini);
        entityManager().flush();

        assertEquals(CipherAlgorithm.AES, bellini.getCipherAlgorithm());

        attrs = mappingManager.prepareAttrsFromAny(
                bellini,
                null,
                true,
                Boolean.TRUE,
                provision);
        assertEquals("newPassword123", SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs.getRight())));
    }

    @Test
    public void prepareAttrsForLinkedAccount() {
        User vivaldi = userDAO.findByUsername("vivaldi");
        ExternalResource ldap = resourceDAO.find("resource-ldap");
        Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).get();

        LinkedAccount account = entityFactory.newEntity(LinkedAccount.class);
        account.setConnObjectKeyValue("admin");
        account.setResource(ldap);
        account.setOwner(vivaldi);
        account.setPassword("Password321", CipherAlgorithm.AES);
        vivaldi.add(account);

        vivaldi = userDAO.save(vivaldi);
        entityManager().flush();

        // 1. with account password and clear-text default password
        Set<Attribute> attrs = mappingManager.prepareAttrsFromLinkedAccount(
                vivaldi,
                account,
                "Password123",
                true,
                provision);
        assertEquals("admin", AttributeUtil.getStringValue(AttributeUtil.find("cn", attrs)));
        assertEquals("Password321", SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs)));

        // 2. without account password and clear-text default password
        account.setEncodedPassword(null, null);

        attrs = mappingManager.prepareAttrsFromLinkedAccount(
                vivaldi,
                account,
                "Password123",
                true,
                provision);
        assertEquals("Password123", SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs)));

        // 3. with changePwd == false
        attrs = mappingManager.prepareAttrsFromLinkedAccount(
                vivaldi,
                account,
                "Password123",
                false,
                provision);
        assertNull(AttributeUtil.getPasswordValue(attrs));

        // 4. without account password, no clear-text password but random password generation enabled
        ldap.setRandomPwdIfNotProvided(true);
        ldap = resourceDAO.save(ldap);
        entityManager().flush();

        String encPassword = vivaldi.getPassword();
        attrs = mappingManager.prepareAttrsFromLinkedAccount(
                vivaldi,
                account,
                null,
                true,
                provision);
        assertNotEquals(encPassword, SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attrs)));

        // 5. without account password, no clear-text password and random password generation disabled
        ldap.setRandomPwdIfNotProvided(false);
        resourceDAO.save(ldap);
        entityManager().flush();

        attrs = mappingManager.prepareAttrsFromLinkedAccount(
                vivaldi,
                account,
                null,
                true,
                provision);
        assertNull(AttributeUtil.getPasswordValue(attrs));
    }

    @Test
    public void issueSYNCOPE1583() {
        // 0. create user matching the condition below
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.add(anyTypeClassDAO.find("other"));

        UPlainAttr cool = entityFactory.newEntity(UPlainAttr.class);
        cool.setOwner(user);
        cool.setSchema(plainSchemaDAO.find("cool"));
        cool.add("true", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(cool);

        user = userDAO.save(user);
        String newUserKey = user.getKey();
        assertNotNull(newUserKey);

        // 1. update group with dynamic membership
        Group group = groupDAO.findByName("root");
        assertNotNull(group);

        UDynGroupMembership dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
        dynMembership.setFIQLCond("cool==true");
        dynMembership.setGroup(group);
        group.setUDynMembership(dynMembership);

        group = groupDAO.saveAndRefreshDynMemberships(group);
        assertNotNull(group);

        entityManager().flush();

        // 2. verify that dynamic membership is in place
        assertTrue(userDAO.findAllGroupKeys(user).contains(group.getKey()));

        // 3. check propagation attrs
        ExternalResource csv = resourceDAO.find("resource-csv");
        Provision provision = csv.getProvision(AnyTypeKind.USER.name()).get();
        assertNotNull(provision);

        Pair<String, Set<Attribute>> attrs = mappingManager.prepareAttrsFromAny(
                user,
                null,
                false,
                Boolean.TRUE,
                provision);
        assertTrue(attrs.getRight().stream().anyMatch(
                attr -> "theirgroup".equals(attr.getName()) && List.of("sx-dx").equals(attr.getValue())));
    }
}
