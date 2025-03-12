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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExceptionMapperITCase extends AbstractITCase {

    private static final Properties ERROR_MESSAGES = new Properties();

    @BeforeAll
    public static void setUpErrorMessages() throws IOException {
        try (InputStream propStream = ExceptionMapperITCase.class.getResourceAsStream("/errorMessages.properties")) {
            ERROR_MESSAGES.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /errorMessages.properties", e);
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
        typeClass.setKey("uniqueAttribute" + getUUIDString());
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        ANY_TYPE_CLASS_SERVICE.create(typeClass);

        // 2. create an user with mandatory attributes and unique
        UserCR userTO1 = new UserCR();
        userTO1.setRealm(SyncopeConstants.ROOT_REALM);
        userTO1.getAuxClasses().add(typeClass.getKey());
        String userId1 = getUUIDString() + "issue654_1@syncope.apache.org";
        userTO1.setUsername(userId1);
        userTO1.setPassword("password123");

        userTO1.getPlainAttrs().add(attr("userId", userId1));
        userTO1.getPlainAttrs().add(attr("fullname", userId1));
        userTO1.getPlainAttrs().add(attr("surname", userId1));
        userTO1.getPlainAttrs().add(attr("unique" + schemaUID, "unique" + schemaUID));

        createUser(userTO1);

        // 3. create an other user with mandatory attributes and unique with the same value of userTO1
        UserCR userTO2 = new UserCR();
        userTO2.setRealm(SyncopeConstants.ROOT_REALM);
        userTO2.getAuxClasses().add(typeClass.getKey());
        String userId2 = getUUIDString() + "issue654_2@syncope.apache.org";
        userTO2.setUsername(userId2);
        userTO2.setPassword("password123");

        userTO2.getPlainAttrs().add(attr("userId", userId2));
        userTO2.getPlainAttrs().add(attr("fullname", userId2));
        userTO2.getPlainAttrs().add(attr("surname", userId2));
        userTO2.getPlainAttrs().add(attr("unique" + schemaUID, "unique" + schemaUID));

        try {
            createUser(userTO2);
            fail("This should not happen");
        } catch (Exception e) {
            String message = ERROR_MESSAGES.getProperty("errMessage.UniqueConstraintViolation");
            assertEquals("EntityExists [" + message + ']', e.getMessage());
        }
    }

    @Test
    public void sameGroupName() {
        String groupUUID = getUUIDString();

        // Create the first group
        GroupCR groupTO1 = new GroupCR();
        groupTO1.setName("child1" + groupUUID);
        groupTO1.setRealm(SyncopeConstants.ROOT_REALM);
        createGroup(groupTO1);

        // Create the second group, with the same name of groupTO1
        GroupCR groupTO2 = new GroupCR();
        groupTO2.setName("child1" + groupUUID);
        groupTO2.setRealm(SyncopeConstants.ROOT_REALM);
        try {
            createGroup(groupTO2);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
            String message = ERROR_MESSAGES.getProperty("errMessage.UniqueConstraintViolation");
            assertEquals("EntityExists [" + message + ']', e.getMessage());
        }
    }

    @Test
    public void headersMultiValue() {
        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "issue654@syncope.apache.org";
        userCR.setUsername(userId);
        userCR.setPassword("password123");

        userCR.getPlainAttrs().add(attr("userId", "issue654"));
        userCR.getPlainAttrs().add(attr("fullname", userId));
        userCR.getPlainAttrs().add(attr("surname", userId));

        try {
            createUser(userCR);
            fail("This should not happen");
        } catch (SyncopeClientCompositeException e) {
            assertEquals(2, e.getExceptions().size());
        }
    }

    @Test
    public void invalidRequests() {
        try {
            TASK_SERVICE.search(new TaskQuery.Builder(TaskType.NOTIFICATION).resource(RESOURCE_NAME_LDAP).build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRequest, e.getType());
        }
        try {
            TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PULL).anyTypeKind(AnyTypeKind.ANY_OBJECT).build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRequest, e.getType());
        }
        try {
            TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PULL).
                    notification("e00945b5-1184-4d43-8e45-4318a8dcdfd4").build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRequest, e.getType());
        }

        try {
            ANY_TYPE_SERVICE.delete(AnyTypeKind.USER.name());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRequest, e.getType());
        }

        try {
            ANONYMOUS_CLIENT.getService(AccessTokenService.class).login();
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRequest, e.getType());
        }

        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        assertTrue(ldap.getProvisions().getFirst().getMapping().getConnObjectKeyItem().isPresent());
        try {
            Item mapping = ldap.getProvisions().getFirst().getMapping().getItems().getFirst();
            mapping.setIntAttrName("memberships.cn");
            RESOURCE_SERVICE.update(ldap);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMapping, e.getType());
        }
        ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        assertTrue(ldap.getProvisions().getFirst().getMapping().getConnObjectKeyItem().isPresent());
    }
}
