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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import javax.validation.ValidationException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.junit.Test;
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

    @Test
    public void findByKey() {
        UPlainAttr attribute = plainAttrDAO.find("01f22fbd-b672-40af-b528-686d9b27ebc4", UPlainAttr.class);
        assertNotNull("did not find expected attribute", attribute);
        attribute = plainAttrDAO.find("9d0d9e40-1b18-488e-9482-37dab82163c9", UPlainAttr.class);
        assertNotNull("did not find expected attribute", attribute);
    }

    @Test
    public void read() {
        UPlainAttr attribute = plainAttrDAO.find("01f22fbd-b672-40af-b528-686d9b27ebc4", UPlainAttr.class);
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
            attr.add("john.doe@gmail.com", anyUtilsFactory.getInstance(AnyTypeKind.USER));
            attr.add("mario.rossi@gmail.com", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNull("no validation exception expected here ", thrown);

        try {
            attr.add("http://www.apache.org", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);
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
            attribute.add("A", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        } catch (ValidationException e) {
            thrown = e;
        }
        assertNotNull("validation exception expected here ", thrown);

        attribute.add("M", anyUtilsFactory.getInstance(AnyTypeKind.USER));

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
            fail();
        } catch (InvalidEntityException e) {
            iee = e;
        }
        assertNotNull(iee);
        // for attribute
        assertTrue(iee.hasViolation(EntityViolationType.InvalidValueList));
        // for uauv
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
        attr.add("testvalue", anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);

        UPlainAttr obscure = user.getPlainAttr("obscure").get();
        assertNotNull(obscure);
        assertEquals(1, obscure.getValues().size());
        assertEquals(Encryptor.getInstance(obscureSchema.getSecretKey()).
                encode("testvalue", obscureSchema.getCipherAlgorithm()), obscure.getValues().get(0).getStringValue());
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
        attr.add(photoB64Value, anyUtilsFactory.getInstance(AnyTypeKind.USER));
        user.add(attr);

        userDAO.save(user);

        UPlainAttr photo = user.getPlainAttr("photo").get();
        assertNotNull(photo);
        assertEquals(1, photo.getValues().size());
        assertTrue(Arrays.equals(bytes, photo.getValues().get(0).getBinaryValue()));
    }

    @Test
    public void delete() {
        UPlainAttr attribute = plainAttrDAO.find(
                "9d0d9e40-1b18-488e-9482-37dab82163c9", UPlainAttr.class);
        String attrSchemaName = attribute.getSchema().getKey();

        plainAttrDAO.delete(attribute.getKey(), UPlainAttr.class);

        PlainSchema schema = plainSchemaDAO.find(attrSchemaName);
        assertNotNull("user attribute schema deleted when deleting values", schema);
    }
}
