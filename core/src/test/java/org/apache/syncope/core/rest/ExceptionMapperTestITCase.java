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
package org.apache.syncope.core.rest;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.SyncopeClientCompositeException;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SchemaType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ExceptionMapperTestITCase extends AbstractTest {

    private static final Properties props = new Properties();

    @BeforeClass
    public static void setUpErrorMessages() throws IOException {
        InputStream propStream = null;
        try {
            propStream = ExceptionMapperTestITCase.class.getResourceAsStream("/errorMessages.properties");
            props.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /errorMessages.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }

    @Test
    public void uniqueSchemaConstraint() {
        // 1. create an user schema with unique constraint
        SchemaTO schemaTO = new SchemaTO();
        String schema_UID = getUUIDString();
        schemaTO.setName("unique" + schema_UID);
        schemaTO.setType(AttributeSchemaType.String);
        schemaTO.setUniqueConstraint(true);
        createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);

        // 2. create an user with mandatory attributes and unique
        UserTO userTO_1 = new UserTO();
        String userId_1 = getUUIDString() + "issue654_1@syncope.apache.org";
        userTO_1.setUsername(userId_1);
        userTO_1.setPassword("password");

        userTO_1.getAttrs().add(attributeTO("userId", userId_1));
        userTO_1.getAttrs().add(attributeTO("fullname", userId_1));
        userTO_1.getAttrs().add(attributeTO("surname", userId_1));
        userTO_1.getAttrs().add(attributeTO("unique" + schema_UID, "unique" + schema_UID));

        createUser(userTO_1);

        // 3. create an other user with mandatory attributes and unique with the same value of userTO_1
        UserTO userTO_2 = new UserTO();
        String userId_2 = getUUIDString() + "issue654_2@syncope.apache.org";
        userTO_2.setUsername(userId_2);
        userTO_2.setPassword("password");

        userTO_2.getAttrs().add(attributeTO("userId", userId_2));
        userTO_2.getAttrs().add(attributeTO("fullname", userId_2));
        userTO_2.getAttrs().add(attributeTO("surname", userId_2));
        userTO_2.getAttrs().add(attributeTO("unique" + schema_UID, "unique" + schema_UID));

        try {
            createUser(userTO_2);
        } catch (Exception e) {
            String message = props.getProperty("errMessage.UniqueConstraintViolation");
            Assert.assertEquals(e.getMessage(), "DataIntegrityViolation [" + message + "]");
        }
    }

    @Test
    public void sameRoleName() {
        //Create the first role
        RoleTO roleTO_1 = new RoleTO();
        String role_UUID = getUUIDString();
        roleTO_1.setName("child1" + role_UUID);
        roleTO_1.setParent(1L);
        createRole(roleTO_1);
        //Create the second role, with the same parent and the same role of roleTO_1
        RoleTO roleTO_2 = new RoleTO();
        roleTO_2.setName("child1" + role_UUID);
        roleTO_2.setParent(1L);
        try {
            createRole(roleTO_2);
        } catch (Exception e) {
            String message = props.getProperty("errMessage.UniqueConstraintViolation");
            Assert.assertEquals(e.getMessage(), "DataIntegrityViolation [" + message + "]");
        }
    }

    @Test
    public void headersMultiValue() {
        UserTO userTO = new UserTO();
        String userId = getUUIDString() + "issue654@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password");

        userTO.getAttrs().add(attributeTO("userId", "issue654"));
        userTO.getAttrs().add(attributeTO("fullname", userId));
        userTO.getAttrs().add(attributeTO("surname", userId));

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientCompositeException e) {
            Assert.assertEquals(2, e.getExceptions().size());
        }
    }

}
