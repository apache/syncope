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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.AttributableOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PlainSchemaITCase extends AbstractITCase {

    private PlainSchemaTO buildPlainSchemaTO(final String name, final AttrSchemaType type) {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(name + getUUIDString());
        schemaTO.setType(type);
        return schemaTO;
    }

    @Test
    public void create() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("testAttribute", AttrSchemaType.String);
        schemaTO.setMandatoryCondition("false");

        PlainSchemaTO newPlainSchemaTO = createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        newPlainSchemaTO = createSchema(AttributableType.MEMBERSHIP, SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);
    }

    @Test
    public void createWithNotPermittedName() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("failedLogins");
        schemaTO.setType(AttrSchemaType.String);

        try {
            createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void createREnumWithoutEnumeration() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("enumcheck");
        schemaTO.setType(AttrSchemaType.Enum);

        try {
            createSchema(AttributableType.GROUP, SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidSchemaEnum.name()));
        }
    }

    @Test
    public void createUEnumWithoutEnumeration() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("enumcheck");
        schemaTO.setType(AttrSchemaType.Enum);

        try {
            createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());

            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidSchemaEnum.name()));
        }
    }

    @Test
    public void createEncrypted() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("encrypted");
        schemaTO.setType(AttrSchemaType.Encrypted);
        schemaTO.setCipherAlgorithm(CipherAlgorithm.AES);
        schemaTO.setSecretKey("huhadfhsjfsfsdkj!####");

        createSchema(AttributableType.MEMBERSHIP, SchemaType.PLAIN, schemaTO);
    }

    @Test
    public void createBinary() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("x509certificate");
        schemaTO.setType(AttrSchemaType.Binary);
        schemaTO.setMimeType("application/x-x509-ca-cert");

        createSchema(AttributableType.GROUP, SchemaType.PLAIN, schemaTO);
    }

    @Test
    public void delete() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("todelete", AttrSchemaType.String);
        schemaTO.setMandatoryCondition("false");
        createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);

        schemaService.delete(AttributableType.USER, SchemaType.PLAIN, schemaTO.getKey());
        PlainSchemaTO firstname = null;
        try {
            firstname = schemaService.read(AttributableType.USER, SchemaType.PLAIN, schemaTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        assertNull(firstname);
    }

    @Test
    public void list() {
        List<PlainSchemaTO> userSchemas = schemaService.list(AttributableType.USER, SchemaType.PLAIN);
        assertFalse(userSchemas.isEmpty());
        for (PlainSchemaTO schemaTO : userSchemas) {
            assertNotNull(schemaTO);
        }

        List<PlainSchemaTO> groupSchemas = schemaService.list(AttributableType.GROUP, SchemaType.PLAIN);
        assertFalse(groupSchemas.isEmpty());
        for (PlainSchemaTO schemaTO : groupSchemas) {
            assertNotNull(schemaTO);
        }

        List<PlainSchemaTO> membershipSchemas = schemaService.list(AttributableType.MEMBERSHIP, SchemaType.PLAIN);
        assertFalse(membershipSchemas.isEmpty());
        for (PlainSchemaTO schemaTO : membershipSchemas) {
            assertNotNull(schemaTO);
        }
    }

    @Test
    public void update() {
        PlainSchemaTO schemaTO = schemaService.read(AttributableType.GROUP, SchemaType.PLAIN, "icon");
        assertNotNull(schemaTO);

        schemaService.update(AttributableType.GROUP, SchemaType.PLAIN, schemaTO.getKey(), schemaTO);
        PlainSchemaTO updatedTO = schemaService.read(AttributableType.GROUP, SchemaType.PLAIN, "icon");
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(AttrSchemaType.Date);
        try {
            schemaService.update(AttributableType.GROUP, SchemaType.PLAIN, schemaTO.getKey(), updatedTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issue258() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("schema_issue258");
        schemaTO.setType(AttrSchemaType.Double);

        schemaTO = createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue258@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1.2"));

        userTO = createUser(userTO);
        assertNotNull(userTO);

        schemaTO.setType(AttrSchemaType.Long);
        try {
            schemaService.update(AttributableType.USER, SchemaType.PLAIN, schemaTO.getKey(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issue259() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("schema_issue259", AttrSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue259@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1"));
        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserTO newUserTO = SerializationUtils.clone(userTO);
        MembershipTO membership = new MembershipTO();
        membership.setGroupKey(2L);
        newUserTO.getMemberships().add(membership);

        UserMod userMod = AttributableOperations.diff(newUserTO, userTO);

        userTO = userService.update(userMod.getKey(), userMod).readEntity(UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public void issue260() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("schema_issue260", AttrSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue260@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1.2"));
        userTO = createUser(userTO);
        assertNotNull(userTO);

        schemaTO.setUniqueConstraint(false);
        try {
            schemaService.update(AttributableType.USER, SchemaType.PLAIN, schemaTO.getKey(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE323() {
        PlainSchemaTO actual = schemaService.read(AttributableType.GROUP, SchemaType.PLAIN, "icon");
        assertNotNull(actual);

        try {
            createSchema(AttributableType.GROUP, SchemaType.PLAIN, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setKey(null);
        try {
            createSchema(AttributableType.GROUP, SchemaType.PLAIN, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE418() {
        PlainSchemaTO schema = buildPlainSchemaTO("http://schemas.examples.org/security/authorization/organizationUnit",
                AttrSchemaType.Double);

        try {
            createSchema(AttributableType.GROUP, SchemaType.PLAIN, schema);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void anonymous() {
        SchemaService unauthenticated = clientFactory.createAnonymous().getService(SchemaService.class);
        try {
            unauthenticated.list(AttributableType.USER, SchemaType.VIRTUAL);
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        SchemaService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(SchemaService.class);
        assertFalse(anonymous.list(AttributableType.USER, SchemaType.VIRTUAL).isEmpty());
    }
}
