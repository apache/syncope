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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.utils.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.junit.Test;
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

    @Test
    public void findAll() {
        List<User> list = userDAO.findAll(1, 100);
        assertEquals("did not get expected number of users", 5, list.size());
    }

    @Test
    public void count() {
        Integer count = userDAO.count();
        assertNotNull(count);
        assertEquals(5, count, 0);
    }

    @Test
    public void findAllByPageAndSize() {
        // get first page
        List<User> list = userDAO.findAll(1, 2);
        assertEquals("did not get expected number of users", 2, list.size());

        // get second page
        list = userDAO.findAll(2, 2);
        assertEquals("did not get expected number of users", 2, list.size());

        // get second page with uncomplete set
        list = userDAO.findAll(2, 3);
        assertEquals("did not get expected number of users", 2, list.size());

        // get unexistent page
        list = userDAO.findAll(3, 2);
        assertEquals("did not get expected number of users", 1, list.size());
    }

    @Test
    public void findByDerAttributeValue() {
        final List<User> list = userDAO.findByDerAttrValue("cn", "Vivaldi, Antonio");
        assertEquals("did not get expected number of users", 1, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByInvalidDerAttrValue() {
        userDAO.findByDerAttrValue("cn", "Antonio, Maria, Rossi");
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByInvalidDerAttrExpression() {
        userDAO.findByDerAttrValue("noschema", "Antonio, Maria");
    }

    @Test
    public void findByAttributeValue() {
        final UPlainAttrValue fullnameValue = entityFactory.newEntity(UPlainAttrValue.class);
        fullnameValue.setStringValue("Gioacchino Rossini");

        final List<User> list = userDAO.findByPlainAttrValue("fullname", fullnameValue);
        assertEquals("did not get expected number of users", 1, list.size());
    }

    @Test
    public void findByAttributeBooleanValue() {
        final UPlainAttrValue coolValue = entityFactory.newEntity(UPlainAttrValue.class);
        coolValue.setBooleanValue(true);

        final List<User> list = userDAO.findByPlainAttrValue("cool", coolValue);
        assertEquals("did not get expected number of users", 1, list.size());
    }

    @Test
    public void findByKey() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull("did not find expected user", user);
    }

    @Test
    public void findByUsername() {
        User user = userDAO.findByUsername("rossini");
        assertNotNull("did not find expected user", user);
        user = userDAO.findByUsername("vivaldi");
        assertNotNull("did not find expected user", user);
        user = userDAO.findByUsername("user6");
        assertNull("found user but did not expect it", user);
    }

    @Test
    public void save() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(new Date());

        user.setPassword("pass", CipherAlgorithm.SHA256);

        try {
            userDAO.save(user);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }

        user.setPassword("password123", CipherAlgorithm.SHA256);

        user.setUsername("username!");

        try {
            userDAO.save(user);
            fail();
        } catch (InvalidEntityException e) {
            assertNotNull(e);
        }

        user.setUsername("username");

        User actual = userDAO.save(user);
        assertNotNull("expected save to work", actual);
        assertEquals(1, actual.getPasswordHistory().size());
        assertNotNull(userDAO.findLastChange(actual.getKey()));
        assertEquals(actual.getLastChangeDate(), userDAO.findLastChange(actual.getKey()));
    }

    @Test
    public void delete() {
        User user = userDAO.find("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");

        userDAO.delete(user.getKey());

        User actual = userDAO.find("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");
        assertNull("delete did not work", actual);
    }

    @Test
    public void issue237() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setRealm(realmDAO.findByFullPath("/even/two"));
        user.setCreator("admin");
        user.setCreationDate(new Date());

        user.setPassword("password123", CipherAlgorithm.AES);

        User actual = userDAO.save(user);
        assertNotNull(actual);
    }

    @Test
    public void issueSYNCOPE391() {
        User user = entityFactory.newEntity(User.class);
        user.setUsername("username");
        user.setPassword(null, CipherAlgorithm.AES);
        user.setRealm(realmDAO.findByFullPath("/even/two"));

        User actual = userDAO.save(user);
        assertNull(user.getPassword());
        assertNotNull(actual);
    }

    @Test
    public void issueSYNCOPE226() {
        User user = userDAO.find("823074dc-d280-436d-a7dd-07399fae48ec");
        String password = "";
        try {
            password = passwordGenerator.generate(user);
        } catch (InvalidPasswordRuleConf ex) {
            fail(ex.getMessage());
        }
        assertNotNull(password);

        user.setPassword(password, CipherAlgorithm.AES);

        User actual = userDAO.save(user);
        assertNotNull(actual);
    }

    @Test
    public void testPasswordGenerator() {
        User user = userDAO.find("823074dc-d280-436d-a7dd-07399fae48ec");

        String password = "";
        try {
            password = passwordGenerator.generate(user);

        } catch (InvalidPasswordRuleConf ex) {
            fail(ex.getMessage());
        }
        assertNotNull(password);
        user.setPassword(password, CipherAlgorithm.SHA);
        userDAO.save(user);
    }
}
