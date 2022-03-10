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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class UserTest extends AbstractTest {

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Test
    public void find() {
        User user = userDAO.find("823074dc-d280-436d-a7dd-07399fae48ec");
        assertNotNull(user);
        assertEquals("puccini", user.getUsername());
        assertFalse(user.isSuspended());
        assertFalse(user.isMustChangePassword());
        assertEquals("active", user.getStatus());
        assertEquals(CipherAlgorithm.SHA1, user.getCipherAlgorithm());
        assertEquals("e4c28e7a-9dbf-4ee7-9441-93812a0d4a28", user.getRealm().getKey());
        assertNull(user.getSecurityQuestion());
        assertNull(user.getSecurityAnswer());
        assertEquals("admin", user.getCreator());
        assertEquals("Giacomo", user.getPlainAttr("firstname").get().getValuesAsStrings().get(0));
        assertEquals("Puccini", user.getPlainAttr("surname").get().getValuesAsStrings().get(0));
    }

    @Test
    public void findAll() {
        List<User> users = userDAO.findAll(1, 100);
        assertEquals(5, users.size());

        List<String> userKeys = userDAO.findAllKeys(1, 100);
        assertNotNull(userKeys);

        assertEquals(users.size(), userKeys.size());
    }

    @Test
    public void count() {
        int count = userDAO.count();
        assertNotNull(count);
        assertEquals(5, count);
    }

    @Test
    public void findAllByPageAndSize() {
        // get first page
        List<User> list = userDAO.findAll(1, 2);
        assertEquals(2, list.size());

        // get second page
        list = userDAO.findAll(2, 2);
        assertEquals(2, list.size());

        // get second page with uncomplete set
        list = userDAO.findAll(2, 3);
        assertEquals(2, list.size());

        // get unexistent page
        list = userDAO.findAll(3, 2);
        assertEquals(1, list.size());
    }

    @Test
    public void findByDerAttrValue() {
        List<User> list = userDAO.findByDerAttrValue(derSchemaDAO.find("cn"), "Vivaldi, Antonio", false);
        assertEquals(1, list.size());

        list = userDAO.findByDerAttrValue(derSchemaDAO.find("cn"), "VIVALDI, ANTONIO", false);
        assertEquals(0, list.size());

        list = userDAO.findByDerAttrValue(derSchemaDAO.find("cn"), "VIVALDI, ANTONIO", true);
        assertEquals(1, list.size());
    }

    @Test
    public void findByInvalidDerAttrValue() {
        assertTrue(userDAO.findByDerAttrValue(derSchemaDAO.find("cn"), "Antonio, Maria, Rossi", false).isEmpty());
    }

    @Test
    public void findByInvalidDerAttrExpression() {
        assertTrue(userDAO.findByDerAttrValue(derSchemaDAO.find("noschema"), "Antonio, Maria", false).isEmpty());
    }

    @Test
    public void findByPlainAttrUniqueValue() {
        UPlainAttrUniqueValue fullnameValue = entityFactory.newEntity(UPlainAttrUniqueValue.class);
        fullnameValue.setStringValue("Gioacchino Rossini");

        PlainSchema fullname = plainSchemaDAO.find("fullname");

        Optional<User> found = userDAO.findByPlainAttrUniqueValue(fullname, fullnameValue, false);
        assertTrue(found.isPresent());

        fullnameValue.setStringValue("Gioacchino ROSSINI");

        found = userDAO.findByPlainAttrUniqueValue(fullname, fullnameValue, false);
        assertFalse(found.isPresent());

        found = userDAO.findByPlainAttrUniqueValue(fullname, fullnameValue, true);
        assertTrue(found.isPresent());
    }

    @Test
    public void findByPlainAttrBooleanValue() {
        final UPlainAttrValue coolValue = entityFactory.newEntity(UPlainAttrValue.class);
        coolValue.setBooleanValue(true);

        final List<User> list = userDAO.findByPlainAttrValue(plainSchemaDAO.find("cool"), coolValue, false);
        assertEquals(1, list.size());
    }

    @Test
    public void findByKey() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(user);
    }

    @Test
    public void findByUsername() {
        User user = userDAO.findByUsername("rossini");
        assertNotNull(user);
        user = userDAO.findByUsername("vivaldi");
        assertNotNull(user);
        user = userDAO.findByUsername("user6");
        assertNull(user);
    }

    @Test
    public void findMembership() {
        UMembership memb = userDAO.findMembership("3d5e91f6-305e-45f9-ad30-4897d3d43bd9");
        assertNotNull(memb);
        assertEquals("1417acbe-cbf6-4277-9372-e75e04f97000", memb.getLeftEnd().getKey());
    }

    @Test
    public void saveInvalidPassword() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user.setPassword("pass");

        try {
            userDAO.save(user);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void saveInvalidUsername() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username!");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user.setPassword("password123");

        try {
            userDAO.save(user);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void save() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user.setPassword("password123");

        User actual = userDAO.save(user);
        assertNotNull(actual);
        assertEquals(1, actual.getPasswordHistory().size());
        assertNotNull(userDAO.findLastChange(actual.getKey()));
        assertTrue(actual.getLastChangeDate().truncatedTo(ChronoUnit.SECONDS).
                isEqual(userDAO.findLastChange(actual.getKey()).truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    public void delete() {
        User user = userDAO.find("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");

        userDAO.delete(user.getKey());

        User actual = userDAO.find("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        assertNull(actual);
    }

    @Test
    public void issue237() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
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
        user.setRealm(realmDAO.findByFullPath("/even/two"));

        User actual = userDAO.save(user);
        assertNull(user.getPassword());
        assertNotNull(actual);
    }

    @Test
    public void testPasswordGenerator() {
        String password = "";
        try {
            password = passwordGenerator.generate(resourceDAO.find("ws-target-resource-nopropagation"));
        } catch (InvalidPasswordRuleConf e) {
            fail(e::getMessage);
        }
        assertNotNull(password);

        User user = userDAO.find("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        user.setPassword(password);
        userDAO.save(user);
    }

    @Test
    public void passwordGeneratorFailing() {
        assertThrows(IllegalArgumentException.class, () -> {
            String password = "";
            try {
                password = passwordGenerator.generate(resourceDAO.find("ws-target-resource-nopropagation"));
            } catch (InvalidPasswordRuleConf e) {
                fail(e.getMessage());
            }
            assertNotNull(password);

            User user = userDAO.find("c9b2dec2-00a7-4855-97c0-d854842b4b24");
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
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(OffsetDateTime.now());
        user.setCipherAlgorithm(CipherAlgorithm.SSHA256);
        user.setPassword("password123");
        user.setSecurityQuestion(securityQuestionDAO.find("887028ea-66fc-41e7-b397-620d7ea6dfbb"));
        String securityAnswer = "my complex answer to @ $complex question è ? £12345";
        user.setSecurityAnswer(securityAnswer);

        User actual = userDAO.save(user);
        assertNotNull(actual);
        assertNotNull(actual.getSecurityAnswer());
        assertTrue(Encryptor.getInstance().verify(securityAnswer, CipherAlgorithm.SSHA256, actual.getSecurityAnswer()));
    }
}
