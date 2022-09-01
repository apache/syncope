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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import javax.validation.ValidationException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainAttrTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Tag("plainAttrTable")
    @Test
    public void findByKey() {
        UPlainAttr attribute = findPlainAttr("01f22fbd-b672-40af-b528-686d9b27ebc4", UPlainAttr.class);
        assertNotNull(attribute);
        attribute = findPlainAttr("9d0d9e40-1b18-488e-9482-37dab82163c9", UPlainAttr.class);
        assertNotNull(attribute);
    }

    @Tag("plainAttrTable")
    @Test
    public void read() {
        UPlainAttr attribute = findPlainAttr("01f22fbd-b672-40af-b528-686d9b27ebc4", UPlainAttr.class);
        assertNotNull(attribute);
        assertTrue(attribute.getValues().isEmpty());
        assertNotNull(attribute.getUniqueValue());
    }

    @Test
    public void save() throws ClassNotFoundException {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");

        PlainSchema emailSchema = plainSchemaDAO.find("email");
        assertNotNull(emailSchema);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(emailSchema);

        Exception thrown = null;
        try {
            attr.add(validator, "john.doe@gmail.com", anyUtilsFactory.getInstance(AnyTypeKind.USER));
            attr.add(validator, "mario.rossi@gmail.com", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull(thrown);

        try {
            attr.add(validator, "http://www.apache.org", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull(thrown);
    }

    @Test
    public void saveWithEnum() throws ClassNotFoundException {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(user);

        PlainSchema gender = plainSchemaDAO.find("gender");
        assertNotNull(gender);
        assertNotNull(gender.getType());
        assertNotNull(gender.getEnumerationValues());

        UPlainAttr attribute = entityFactory.newEntity(UPlainAttr.class);
        attribute.setOwner(user);
        attribute.setSchema(gender);
        user.add(attribute);

        Exception thrown = null;
        try {
            attribute.add(validator, "A", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull(thrown);

        attribute.add(validator, "M", anyUtilsFactory.getInstance(AnyTypeKind.USER));

        InvalidEntityException iee = null;
        try {
            userDAO.save(user);
        } catch (InvalidEntityException e) {
            iee = e;
        }
        assertNull(iee);
    }

    @Test
    public void invalidValueList() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");

        PlainSchema emailSchema = plainSchemaDAO.find("email");
        assertNotNull(emailSchema);

        PlainSchema fullnameSchema = plainSchemaDAO.find("fullname");
        assertNotNull(fullnameSchema);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(emailSchema);

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

    @Tag("plainAttrTable")
    @Test
    public void invalidPlainAttr() {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");

        PlainSchema emailSchema = plainSchemaDAO.find("email");
        assertNotNull(emailSchema);

        PlainSchema fullnameSchema = plainSchemaDAO.find("fullname");
        assertNotNull(fullnameSchema);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(emailSchema);

        UPlainAttrUniqueValue uauv = entityFactory.newEntity(UPlainAttrUniqueValue.class);
        uauv.setAttr(attr);
        uauv.setSchema(fullnameSchema);
        uauv.setStringValue("a value");

        attr.setUniqueValue(uauv);

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
        // for uauv because uauv.schema and uauv.attr.schema are different
        assertTrue(iee.hasViolation(EntityViolationType.InvalidPlainAttr));
    }

    @Test
    public void saveWithEncrypted() throws Exception {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");

        PlainSchema obscureSchema = plainSchemaDAO.find("obscure");
        assertNotNull(obscureSchema);
        assertNotNull(obscureSchema.getSecretKey());
        assertNotNull(obscureSchema.getCipherAlgorithm());

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(obscureSchema);
        attr.add(validator, "testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);

        UPlainAttr obscure = user.getPlainAttr("obscure").get();
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertEquals(Encryptor.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), obscure.getValues().get(0).getStringValue());
    }

    @Test
    public void encryptedWithKeyAsSysProp() throws Exception {
        PlainSchema obscureSchema = plainSchemaDAO.find("obscure");
        assertNotNull(obscureSchema);

        PlainSchema obscureWithKeyAsSysprop = entityFactory.newEntity(PlainSchema.class);
        obscureWithKeyAsSysprop.setKey("obscureWithKeyAsSysprop");
        obscureWithKeyAsSysprop.setAnyTypeClass(obscureSchema.getAnyTypeClass());
        obscureWithKeyAsSysprop.setType(AttrSchemaType.Encrypted);
        obscureWithKeyAsSysprop.setCipherAlgorithm(obscureSchema.getCipherAlgorithm());
        obscureWithKeyAsSysprop.setSecretKey("${obscureSecretKey}");

        obscureWithKeyAsSysprop = plainSchemaDAO.save(obscureWithKeyAsSysprop);

        System.setProperty("obscureSecretKey", obscureSchema.getSecretKey());

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setSchema(obscureWithKeyAsSysprop);
        attr.add(validator, "testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));

        assertEquals(Encryptor.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), attr.getValues().get(0).getStringValue());
    }

    @Test
    public void encryptedWithDecodeConversionPattern() throws Exception {
        PlainSchema obscureWithDecodeConversionPattern = entityFactory.newEntity(PlainSchema.class);
        obscureWithDecodeConversionPattern.setKey("obscureWithDecodeConversionPattern");
        obscureWithDecodeConversionPattern.setAnyTypeClass(anyTypeClassDAO.find("other"));
        obscureWithDecodeConversionPattern.setType(AttrSchemaType.Encrypted);
        obscureWithDecodeConversionPattern.setCipherAlgorithm(CipherAlgorithm.AES);
        obscureWithDecodeConversionPattern.setSecretKey(SecureRandomUtils.generateRandomUUID().toString());

        obscureWithDecodeConversionPattern = plainSchemaDAO.save(obscureWithDecodeConversionPattern);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setSchema(obscureWithDecodeConversionPattern);
        attr.add(validator, "testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));

        assertEquals(Encryptor.getInstance(obscureWithDecodeConversionPattern.getSecretKey()).
                encode("testvalue", obscureWithDecodeConversionPattern.getCipherAlgorithm()),
                attr.getValues().get(0).getStringValue());

        obscureWithDecodeConversionPattern.setConversionPattern(SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN);
        plainSchemaDAO.save(obscureWithDecodeConversionPattern);

        assertNotEquals("testvalue", attr.getValues().get(0).getStringValue());
        assertEquals("testvalue", attr.getValuesAsStrings().get(0));
    }

    @Test
    public void saveWithBinary() throws UnsupportedEncodingException {
        User user = userDAO.find("1417acbe-cbf6-4277-9372-e75e04f97000");

        PlainSchema photoSchema = plainSchemaDAO.find("photo");
        assertNotNull(photoSchema);
        assertNotNull(photoSchema.getMimeType());

        byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        String photoB64Value = Base64.getEncoder().encodeToString(bytes);

        UPlainAttr attr = entityFactory.newEntity(UPlainAttr.class);
        attr.setOwner(user);
        attr.setSchema(photoSchema);
        attr.add(validator, photoB64Value, anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);

        UPlainAttr photo = user.getPlainAttr("photo").get();
        assertNotNull(photo);
        assertEquals(1, photo.getValues().size());
        assertTrue(Arrays.equals(bytes, photo.getValues().get(0).getBinaryValue()));
    }

    @Tag("plainAttrTable")
    @Test
    public void delete() {
        UPlainAttr attribute = findPlainAttr("9d0d9e40-1b18-488e-9482-37dab82163c9", UPlainAttr.class);
        String attrSchemaName = attribute.getSchema().getKey();

        plainAttrDAO.delete(attribute);

        PlainSchema schema = plainSchemaDAO.find(attrSchemaName);
        assertNotNull(schema);
    }
}
