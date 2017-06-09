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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExceptionMapperITCase extends AbstractITCase {

    private static final Properties ERROR_MESSAGES = new Properties();

    @BeforeClass
    public static void setUpErrorMessages() throws IOException {
        InputStream propStream = null;
        try {
            propStream = ExceptionMapperITCase.class.getResourceAsStream("/errorMessages.properties");
            ERROR_MESSAGES.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /errorMessages.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }

    @Test
    public void uniqueSchemaConstraint() {
        // 1. create an user schema with unique constraint
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        String schemaUID = getUUIDString();
        schemaTO.setKey("unique" + schemaUID);
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setUniqueConstraint(true);
        createSchema(SchemaType.PLAIN, schemaTO);

        AnyTypeClassTO typeClass = new AnyTypeClassTO();
        typeClass.setKey("camelAttribute" + getUUIDString());
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        anyTypeClassService.create(typeClass);

        // 2. create an user with mandatory attributes and unique
        UserTO userTO1 = new UserTO();
        userTO1.setRealm(SyncopeConstants.ROOT_REALM);
        userTO1.getAuxClasses().add(typeClass.getKey());
        String userId1 = getUUIDString() + "issue654_1@syncope.apache.org";
        userTO1.setUsername(userId1);
        userTO1.setPassword("password123");

        userTO1.getPlainAttrs().add(attrTO("userId", userId1));
        userTO1.getPlainAttrs().add(attrTO("fullname", userId1));
        userTO1.getPlainAttrs().add(attrTO("surname", userId1));
        userTO1.getPlainAttrs().add(attrTO("unique" + schemaUID, "unique" + schemaUID));

        createUser(userTO1);

        // 3. create an other user with mandatory attributes and unique with the same value of userTO1
        UserTO userTO2 = new UserTO();
        userTO2.setRealm(SyncopeConstants.ROOT_REALM);
        userTO2.getAuxClasses().add(typeClass.getKey());
        String userId2 = getUUIDString() + "issue654_2@syncope.apache.org";
        userTO2.setUsername(userId2);
        userTO2.setPassword("password123");

        userTO2.getPlainAttrs().add(attrTO("userId", userId2));
        userTO2.getPlainAttrs().add(attrTO("fullname", userId2));
        userTO2.getPlainAttrs().add(attrTO("surname", userId2));
        userTO2.getPlainAttrs().add(attrTO("unique" + schemaUID, "unique" + schemaUID));

        try {
            createUser(userTO2);
            fail();
        } catch (Exception e) {
            String message = ERROR_MESSAGES.getProperty("errMessage.UniqueConstraintViolation");
            assertEquals("EntityExists [" + message + "]", e.getMessage());
        }
    }

    @Test
    public void sameGroupName() {
        String groupUUID = getUUIDString();

        // Create the first group
        GroupTO groupTO1 = new GroupTO();
        groupTO1.setName("child1" + groupUUID);
        groupTO1.setRealm(SyncopeConstants.ROOT_REALM);
        createGroup(groupTO1);

        // Create the second group, with the same name of groupTO1
        GroupTO groupTO2 = new GroupTO();
        groupTO2.setName("child1" + groupUUID);
        groupTO2.setRealm(SyncopeConstants.ROOT_REALM);
        try {
            createGroup(groupTO2);
            fail();
        } catch (Exception e) {
            String message = ERROR_MESSAGES.getProperty("errMessage.UniqueConstraintViolation");
            assertEquals("DataIntegrityViolation [" + message + "]", e.getMessage());
        }
    }

    @Test
    public void headersMultiValue() {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "issue654@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password123");

        userTO.getPlainAttrs().add(attrTO("userId", "issue654"));
        userTO.getPlainAttrs().add(attrTO("fullname", userId));
        userTO.getPlainAttrs().add(attrTO("surname", userId));

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertEquals(2, e.getExceptions().size());
        }
    }
}
