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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.validation.ValidationException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlainAttrTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Autowired
    private EncryptorManager encryptorManager;

    @Test
    public void save() throws ClassNotFoundException {
        PlainSchema emailSchema = plainSchemaDAO.findById("email").orElseThrow();

        PlainAttr attr = new PlainAttr();
        attr.setSchema(emailSchema.getKey());

        Exception thrown = null;
        try {
            attr.add(validator, "john.doe@gmail.com");
            attr.add(validator, "mario.rossi@gmail.com");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull(thrown);

        try {
            attr.add(validator, "http://www.apache.org");
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull(thrown);
    }

    @Test
    public void invalidValueList() {
        User user = userDAO.findById("1417acbe-cbf6-4277-9372-e75e04f97000").orElseThrow();

        PlainSchema emailSchema = plainSchemaDAO.findById("email").orElseThrow();

        PlainAttr attr = new PlainAttr();
        attr.setSchema(emailSchema.getKey());

        user.add(attr);

        InvalidEntityException iee = null;
        try {
            userDAO.save(user);
            fail("This should not happen");
        } catch (InvalidEntityException e) {
            iee = e;
        }
        assertNotNull(iee);
        // for attr because no values are set
        assertTrue(iee.hasViolation(EntityViolationType.InvalidValueList));
    }

    @Test
    public void saveWithEncrypted() throws Exception {
        User user = userDAO.findById("1417acbe-cbf6-4277-9372-e75e04f97000").orElseThrow();

        PlainSchema obscureSchema = plainSchemaDAO.findById("obscure").orElseThrow();
        assertNotNull(obscureSchema.getSecretKey());
        assertNotNull(obscureSchema.getCipherAlgorithm());

        PlainAttr attr = new PlainAttr();
        attr.setSchema(obscureSchema.getKey());
        attr.add(validator, "testvalue");
        user.add(attr);

        userDAO.save(user);

        PlainAttr obscure = user.getPlainAttr("obscure").orElseThrow();
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertEquals(encryptorManager.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()),
            obscure.getValues().getFirst().getStringValue());
    }

    @Test
    public void encryptedWithKeyAsSysProp() throws Exception {
        PlainSchema obscureSchema = plainSchemaDAO.findById("obscure").orElseThrow();

        PlainSchema obscureWithKeyAsSysprop = entityFactory.newEntity(PlainSchema.class);
        obscureWithKeyAsSysprop.setKey("obscureWithKeyAsSysprop");
        obscureWithKeyAsSysprop.setAnyTypeClass(obscureSchema.getAnyTypeClass());
        obscureWithKeyAsSysprop.setType(AttrSchemaType.Encrypted);
        obscureWithKeyAsSysprop.setCipherAlgorithm(obscureSchema.getCipherAlgorithm());
        obscureWithKeyAsSysprop.setSecretKey("${obscureSecretKey}");

        obscureWithKeyAsSysprop = plainSchemaDAO.save(obscureWithKeyAsSysprop);

        System.setProperty("obscureSecretKey", obscureSchema.getSecretKey());

        PlainAttr attr = new PlainAttr();
        attr.setSchema(obscureWithKeyAsSysprop.getKey());
        attr.add(validator, "testvalue");

        assertEquals(encryptorManager.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), attr.getValues().getFirst().getStringValue());
    }

    @Test
    public void encryptedWithDecodeConversionPattern() throws Exception {
        PlainSchema obscureWithDecodeConversionPattern = entityFactory.newEntity(PlainSchema.class);
        obscureWithDecodeConversionPattern.setKey("obscureWithDecodeConversionPattern");
        obscureWithDecodeConversionPattern.setAnyTypeClass(anyTypeClassDAO.findById("other").orElseThrow());
        obscureWithDecodeConversionPattern.setType(AttrSchemaType.Encrypted);
        obscureWithDecodeConversionPattern.setCipherAlgorithm(CipherAlgorithm.AES);
        obscureWithDecodeConversionPattern.setSecretKey(SecureRandomUtils.generateRandomUUID().toString());

        obscureWithDecodeConversionPattern = plainSchemaDAO.save(obscureWithDecodeConversionPattern);

        PlainAttr attr = new PlainAttr();
        attr.setSchema(obscureWithDecodeConversionPattern.getKey());
        attr.add(validator, "testvalue");

        assertEquals(encryptorManager.getInstance(obscureWithDecodeConversionPattern.getSecretKey()).
                encode("testvalue", obscureWithDecodeConversionPattern.getCipherAlgorithm()),
                attr.getValues().getFirst().getStringValue());

        obscureWithDecodeConversionPattern.setConversionPattern(SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN);
        plainSchemaDAO.save(obscureWithDecodeConversionPattern);

        assertNotEquals("testvalue", attr.getValues().getFirst().getStringValue());
        assertEquals("testvalue", attr.getValuesAsStrings().getFirst());
    }

    @Test
    public void saveWithBinary() throws UnsupportedEncodingException {
        User user = userDAO.findById("1417acbe-cbf6-4277-9372-e75e04f97000").orElseThrow();

        PlainSchema photoSchema = plainSchemaDAO.findById("photo").orElseThrow();
        assertNotNull(photoSchema.getMimeType());

        byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        String photoB64Value = Base64.getEncoder().encodeToString(bytes);

        PlainAttr attr = new PlainAttr();
        attr.setSchema(photoSchema.getKey());
        attr.add(validator, photoB64Value);
        user.add(attr);

        userDAO.save(user);

        PlainAttr photo = user.getPlainAttr("photo").orElseThrow();
        assertNotNull(photo);
        assertEquals(1, photo.getValues().size());
        assertTrue(Arrays.equals(bytes, photo.getValues().getFirst().getBinaryValue()));
    }
}
