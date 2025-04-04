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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserTest extends AbstractTest {

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private EncryptorManager encryptorManager;

    @Test
    public void find() {
        User user = userDAO.findById("823074dc-d280-436d-a7dd-07399fae48ec").orElseThrow();
        assertEquals("puccini", user.getUsername());
        assertFalse(user.isSuspended());
        assertFalse(user.isMustChangePassword());
        assertEquals("active", user.getStatus());
        assertEquals(CipherAlgorithm.SHA1, user.getCipherAlgorithm());
        assertEquals("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28", user.getRealm().getKey());
        assertNull(user.getSecurityQuestion());
        assertNull(user.getSecurityAnswer());
        assertEquals("admin", user.getCreator());
        assertEquals("Giacomo", user.getPlainAttr("firstname").get().getValuesAsStrings().getFirst());
        assertEquals("Puccini", user.getPlainAttr("surname").get().getValuesAsStrings().getFirst());
    }

    @Test
    public void findUsername() {
        assertEquals("puccini", userDAO.findUsername("823074dc-d280-436d-a7dd-07399fae48ec").orElseThrow());
    }

    @Test
    public void findByToken() {
        assertTrue(userDAO.findByToken("WRONG TOKEN").isEmpty());
    }

    @Test
    public void findAll() {
        List<? extends User> users = userDAO.findAll();
        assertEquals(5, users.size());
    }

    @Test
    public void count() {
        long count = userDAO.count();
        assertNotNull(count);
        assertEquals(5, count);
    }

    @Test
    public void findByDerAttrValue() {
        List<User> list = userDAO.findByDerAttrValue(
                derSchemaDAO.findById("cn").orElseThrow().getExpression(), "Vivaldi, Antonio", false);
        assertEquals(1, list.size());

        list = userDAO.findByDerAttrValue(
                derSchemaDAO.findById("cn").orElseThrow().getExpression(), "VIVALDI, ANTONIO", false);
        assertEquals(0, list.size());

        list = userDAO.findByDerAttrValue(
                derSchemaDAO.findById("cn").orElseThrow().getExpression(), "VIVALDI, ANTONIO", true);
        assertEquals(1, list.size());
    }

    @Test
    public void findByInvalidDerAttrValue() {
        assertTrue(userDAO.findByDerAttrValue(
                derSchemaDAO.findById("cn").orElseThrow().getExpression(), "Antonio, Maria, Rossi", false).isEmpty());
    }

    @Test
    public void findByInvalidDerAttrExpression() {
        assertThrows(IllegalArgumentException.class, () -> userDAO.findByDerAttrValue(
                derSchemaDAO.findById("noschema").orElseThrow().getExpression(), "Antonio, Maria", false).isEmpty());
    }

    @Test
    public void findByKey() {
        assertTrue(userDAO.findById("1417acbe-cbf6-4277-9372-e75e04f97000").isPresent());
    }

    @Test
    public void findByUsername() {
        assertTrue(userDAO.findByUsername("rossini").isPresent());
        assertTrue(userDAO.findByUsername("vivaldi").isPresent());
        assertTrue(userDAO.findByUsername("user6").isEmpty());
    }

    @Test
    public void findMembership() {
        UMembership memb = userDAO.findMembership("3d5e91f6-305e-45f9-ad30-4897d3d43bd9");
        assertNotNull(memb);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", memb.getLeftEnd().getKey());
    }

    @Test
    public void save() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user.setPassword("password123");

        User actual = userDAO.save(user);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        User user = userDAO.findById("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").orElseThrow();

        userDAO.deleteById(user.getKey());

        assertTrue(userDAO.findById("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").isEmpty());
    }

    @Test
    public void issue237() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());

        user.setCipherAlgorithm(CipherAlgorithm.AES);
        user.setPassword("password123");

        User actual = userDAO.save(user);
        assertNotNull(actual);
    }

    @Test
    public void issueSYNCOPE391() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setCipherAlgorithm(CipherAlgorithm.AES);
        user.setPassword(null);
        user.setRealm(realmSearchDAO.findByFullPath("/even/two").orElseThrow());

        User actual = userDAO.save(user);
        assertNull(user.getPassword());
        assertNotNull(actual);
    }

    @Test
    public void testPasswordGenerator() {
        String password = passwordGenerator.generate(
                resourceDAO.findById("ws-target-resource-nopropagation").orElseThrow(),
                List.of(realmDAO.getRoot()));
        assertNotNull(password);

        User user = userDAO.findById("c9b2dec2-00a7-4855-97c0-d854842b4b24").orElseThrow();
        user.setPassword(password);
        userDAO.save(user);
    }

    @Test
    public void passwordGeneratorFailing() {
        assertThrows(IllegalArgumentException.class, () -> {
            String password = passwordGenerator.generate(
                    resourceDAO.findById("ws-target-resource-nopropagation").orElseThrow(),
                    List.of(realmDAO.getRoot()));
            assertNotNull(password);

            User user = userDAO.findById("c9b2dec2-00a7-4855-97c0-d854842b4b24").orElseThrow();
            // SYNCOPE-1666 fail because cipherAlgorithm is already set
            user.setCipherAlgorithm(CipherAlgorithm.SHA);
            user.setPassword(password);
            userDAO.save(user);
        });
    }

    @Test
    public void issueSYNCOPE1666() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmSearchDAO.findByFullPath("/even/two").orElseThrow());
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SSHA256);
        user.setPassword("password123");
        user.setSecurityQuestion(securityQuestionDAO.findById("887028ea-66fc-41e7-b397-620d7ea6dfbb").orElseThrow());
        String securityAnswer = "my complex answer to @ $complex question è ? £12345";
        user.setSecurityAnswer(securityAnswer);

        User actual = userDAO.save(user);
        assertNotNull(actual);
        assertNotNull(actual.getSecurityAnswer());
        assertTrue(encryptorManager.getInstance().
                verify(securityAnswer, CipherAlgorithm.SSHA256, actual.getSecurityAnswer()));
    }
}
