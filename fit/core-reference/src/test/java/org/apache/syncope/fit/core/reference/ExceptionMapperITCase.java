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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ExceptionMapperITCase extends AbstractITCase {

    private static final Properties props = new Properties();

    @BeforeClass
    public static void setUpErrorMessages() throws IOException {
        InputStream propStream = null;
        try {
            propStream = ExceptionMapperITCase.class.getResourceAsStream("/errorMessages.properties");
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
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        String schema_UID = getUUIDString();
        schemaTO.setKey("unique" + schema_UID);
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setUniqueConstraint(true);
        createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);

        // 2. create an user with mandatory attributes and unique
        UserTO userTO_1 = new UserTO();
        String userId_1 = getUUIDString() + "issue654_1@syncope.apache.org";
        userTO_1.setUsername(userId_1);
        userTO_1.setPassword("password");

        userTO_1.getPlainAttrs().add(attrTO("userId", userId_1));
        userTO_1.getPlainAttrs().add(attrTO("fullname", userId_1));
        userTO_1.getPlainAttrs().add(attrTO("surname", userId_1));
        userTO_1.getPlainAttrs().add(attrTO("unique" + schema_UID, "unique" + schema_UID));

        createUser(userTO_1);

        // 3. create an other user with mandatory attributes and unique with the same value of userTO_1
        UserTO userTO_2 = new UserTO();
        String userId_2 = getUUIDString() + "issue654_2@syncope.apache.org";
        userTO_2.setUsername(userId_2);
        userTO_2.setPassword("password");

        userTO_2.getPlainAttrs().add(attrTO("userId", userId_2));
        userTO_2.getPlainAttrs().add(attrTO("fullname", userId_2));
        userTO_2.getPlainAttrs().add(attrTO("surname", userId_2));
        userTO_2.getPlainAttrs().add(attrTO("unique" + schema_UID, "unique" + schema_UID));

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
