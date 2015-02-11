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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import javax.validation.ValidationException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.misc.security.Encryptor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AttrTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainSchemaDAO userSchemaDAO;

    @Test
    public void findById() {
        UPlainAttr attribute = plainAttrDAO.find(100L, UPlainAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
        attribute = plainAttrDAO.find(104L, UPlainAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
    }

    @Test
    public void read() {
        UPlainAttr attribute = plainAttrDAO.find(100L, UPlainAttr.class);
        assertNotNull(attribute);
        assertTrue(attribute.getValues().isEmpty());
        assertNotNull(attribute.getUniqueValue());
    }

    @Test
    public void save() throws ClassNotFoundException {
        User user = userDAO.find(1L);

        UPlainSchema emailSchema = userSchemaDAO.find("email", UPlainSchema.class);
        assertNotNull(emailSchema);

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(emailSchema);
        attribute.setOwner(user);

        Exception thrown = null;
        try {
            attribute.addValue("john.doe@gmail.com", attrUtilFactory.getInstance(AttributableType.USER));
            attribute.addValue("mario.rossi@gmail.com", attrUtilFactory.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);

        try {
            attribute.addValue("http://www.apache.org", attrUtilFactory.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);
    }

    @Test
    public void saveWithEnum() throws ClassNotFoundException {
        User user = userDAO.find(1L);

        UPlainSchema gender = userSchemaDAO.find("gender", UPlainSchema.class);
        assertNotNull(gender);
        assertNotNull(gender.getType());
        assertNotNull(gender.getEnumerationValues());

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(gender);
        attribute.setOwner(user);
        user.addPlainAttr(attribute);

        Exception thrown = null;

        try {
            attribute.addValue("A", attrUtilFactory.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute.addValue("M", attrUtilFactory.getInstance(AttributableType.USER));

        InvalidEntityException iee = null;
        try {
            userDAO.save(user);
        } catch (InvalidEntityException e) {
            iee = e;
        }
        assertNull(iee);
    }

    @Test
    public void validateAndSave() {
        User user = userDAO.find(1L);

        final UPlainSchema emailSchema = userSchemaDAO.find("email", UPlainSchema.class);
        assertNotNull(emailSchema);

        final UPlainSchema fullnameSchema = userSchemaDAO.find("fullname", UPlainSchema.class);
        assertNotNull(fullnameSchema);

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(emailSchema);

        UPlainAttrUniqueValue uauv = entityFactory.newEntity(UPlainAttrUniqueValue.class);
        uauv.setAttr(attribute);
        uauv.setSchema(fullnameSchema);
        uauv.setStringValue("a value");

        attribute.setUniqueValue(uauv);

        user.addPlainAttr(attribute);

        InvalidEntityException iee = null;
        try {
            userDAO.save(user);
            fail();
        } catch (InvalidEntityException e) {
            iee = e;
        }
        assertNotNull(iee);
        // for attribute
        assertTrue(iee.hasViolation(EntityViolationType.InvalidValueList));
        // for uauv
        assertTrue(iee.hasViolation(EntityViolationType.InvalidUPlainSchema));
    }

    @Test
    public void saveWithEncrypted() throws Exception {
        User user = userDAO.find(1L);

        final UPlainSchema obscureSchema = userSchemaDAO.find("obscure", UPlainSchema.class);
        assertNotNull(obscureSchema);
        assertNotNull(obscureSchema.getSecretKey());
        assertNotNull(obscureSchema.getCipherAlgorithm());

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(obscureSchema);
        attribute.addValue("testvalue", attrUtilFactory.getInstance(AttributableType.USER));
        attribute.setOwner(user);
        user.addPlainAttr(attribute);

        userDAO.save(user);

        UPlainAttr obscure = user.getPlainAttr("obscure");
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertEquals(Encryptor.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), obscure.getValues().get(0).getStringValue());
    }

    @Test
    public void saveWithBinary() throws UnsupportedEncodingException {
        User user = userDAO.find(1L);

        final UPlainSchema photoSchema = userSchemaDAO.find("photo", UPlainSchema.class);
        assertNotNull(photoSchema);
        assertNotNull(photoSchema.getMimeType());

        final byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        final String photoB64Value = new String(Base64.encode(bytes), SyncopeConstants.DEFAULT_ENCODING);

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setSchema(photoSchema);
        attribute.addValue(photoB64Value, attrUtilFactory.getInstance(AttributableType.USER));
        attribute.setOwner(user);
        user.addPlainAttr(attribute);

        userDAO.save(user);

        UPlainAttr obscure = user.getPlainAttr("photo");
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertTrue(Arrays.equals(bytes, obscure.getValues().get(0).getBinaryValue()));
    }

    @Test
    public void delete() {
        UPlainAttr attribute = plainAttrDAO.find(104L, UPlainAttr.class);
        String attrSchemaName = attribute.getSchema().getKey();

        plainAttrDAO.delete(attribute.getKey(), UPlainAttr.class);

        UPlainSchema schema = userSchemaDAO.find(attrSchemaName, UPlainSchema.class);
        assertNotNull("user attribute schema deleted when deleting values", schema);
    }
}
