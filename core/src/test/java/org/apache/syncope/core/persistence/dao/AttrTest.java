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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import javax.validation.ValidationException;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.Encryptor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AttrTest extends AbstractDAOTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private SchemaDAO userSchemaDAO;

    @Test
    public void findById() {
        UAttr attribute = attrDAO.find(100L, UAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
        attribute = attrDAO.find(104L, UAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
    }

    @Test
    public void read() {
        UAttr attribute = attrDAO.find(100L, UAttr.class);
        assertNotNull(attribute);

        assertTrue(attribute.getValues().isEmpty());
        assertNotNull(attribute.getUniqueValue());
    }

    @Test
    public void save() throws ClassNotFoundException {
        SyncopeUser user = userDAO.find(1L);

        USchema emailSchema = userSchemaDAO.find("email", USchema.class);
        assertNotNull(emailSchema);

        UAttr attribute = new UAttr();
        attribute.setSchema(emailSchema);
        attribute.setOwner(user);

        Exception thrown = null;
        try {
            attribute.addValue("john.doe@gmail.com", AttributableUtil.getInstance(AttributableType.USER));
            attribute.addValue("mario.rossi@gmail.com", AttributableUtil.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            LOG.error("Unexpected exception", e);
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);

        try {
            attribute.addValue("http://www.apache.org", AttributableUtil.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);
    }

    @Test
    public void saveWithEnum() throws ClassNotFoundException {
        SyncopeUser user = userDAO.find(1L);

        USchema gender = userSchemaDAO.find("gender", USchema.class);
        assertNotNull(gender);
        assertNotNull(gender.getType());
        assertNotNull(gender.getEnumerationValues());

        UAttr attribute = new UAttr();
        attribute.setSchema(gender);
        attribute.setOwner(user);
        user.addAttr(attribute);

        Exception thrown = null;

        try {
            attribute.addValue("A", AttributableUtil.getInstance(AttributableType.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute.addValue("M", AttributableUtil.getInstance(AttributableType.USER));

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
        SyncopeUser user = userDAO.find(1L);

        final USchema emailSchema = userSchemaDAO.find("email", USchema.class);
        assertNotNull(emailSchema);

        final USchema fullnameSchema = userSchemaDAO.find("fullname", USchema.class);
        assertNotNull(fullnameSchema);

        UAttr attribute = new UAttr();
        attribute.setSchema(emailSchema);

        UAttrUniqueValue uauv = new UAttrUniqueValue();
        uauv.setAttribute(attribute);
        uauv.setSchema(fullnameSchema);
        uauv.setStringValue("a value");

        attribute.setUniqueValue(uauv);

        user.addAttr(attribute);

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
        assertTrue(iee.hasViolation(EntityViolationType.InvalidUSchema));
    }

    @Test
    public void saveWithEncrypted() throws Exception {
        SyncopeUser user = userDAO.find(1L);

        final USchema obscureSchema = userSchemaDAO.find("obscure", USchema.class);
        assertNotNull(obscureSchema);
        assertNotNull(obscureSchema.getSecretKey());
        assertNotNull(obscureSchema.getCipherAlgorithm());

        UAttr attribute = new UAttr();
        attribute.setSchema(obscureSchema);
        attribute.addValue("testvalue", AttributableUtil.getInstance(AttributableType.USER));
        attribute.setOwner(user);
        user.addAttr(attribute);

        userDAO.save(user);

        UAttr obscure = user.getAttr("obscure");
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertEquals(Encryptor.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), obscure.getValues().get(0).getStringValue());
    }

    @Test
    public void saveWithBinary() throws UnsupportedEncodingException {
        SyncopeUser user = userDAO.find(1L);

        final USchema photoSchema = userSchemaDAO.find("photo", USchema.class);
        assertNotNull(photoSchema);
        assertNotNull(photoSchema.getMimeType());

        final byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        final String photoB64Value = new String(Base64.encode(bytes), SyncopeConstants.DEFAULT_ENCODING);

        UAttr attribute = new UAttr();
        attribute.setSchema(photoSchema);
        attribute.addValue(photoB64Value, AttributableUtil.getInstance(AttributableType.USER));
        attribute.setOwner(user);
        user.addAttr(attribute);

        userDAO.save(user);

        UAttr obscure = user.getAttr("photo");
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertTrue(Arrays.equals(bytes, obscure.getValues().get(0).getBinaryValue()));
    }

    @Test
    public void delete() {
        UAttr attribute = attrDAO.find(104L, UAttr.class);
        String attrSchemaName = attribute.getSchema().getName();

        attrDAO.delete(attribute.getId(), UAttr.class);

        USchema schema = userSchemaDAO.find(attrSchemaName, USchema.class);
        assertNotNull("user attribute schema deleted when deleting values", schema);
    }
}
