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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.apache.cxf.helpers.IOUtils;

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

        PlainSchemaTO newPlainSchemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void createWithNotPermittedName() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("failedLogins");
        schemaTO.setType(AttrSchemaType.String);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            boolean entityViolationTypeCheck = false;
            for (String element : e.getElements()) {
                if (!entityViolationTypeCheck) {
                    entityViolationTypeCheck = element.contains(EntityViolationType.InvalidKey.name());
                }
            }
            assertTrue(entityViolationTypeCheck);
        }
    }

    @Test
    public void createREnumWithoutEnumeration() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("enumcheck");
        schemaTO.setType(AttrSchemaType.Enum);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidSchemaEnum.name()));
        }
    }

    @Test
    public void createUEnumWithoutEnumeration() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("enumcheck");
        schemaTO.setType(AttrSchemaType.Enum);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
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

        createSchema(SchemaType.PLAIN, schemaTO);
    }

    @Test
    public void createBinary() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("x509certificate");
        schemaTO.setType(AttrSchemaType.Binary);
        schemaTO.setMimeType("application/x-x509-ca-cert");

        createSchema(SchemaType.PLAIN, schemaTO);
    }

    @Test
    public void testBinaryValidation() throws IOException {
        // pdf - with validator
        PlainSchemaTO schemaTOpdf = new PlainSchemaTO();
        schemaTOpdf.setKey("BinaryPDF");
        schemaTOpdf.setType(AttrSchemaType.Binary);
        schemaTOpdf.setMimeType("application/pdf");
        schemaTOpdf.setValidator("BinaryValidator");
        schemaTOpdf.setAnyTypeClass("minimal user");

        createSchema(SchemaType.PLAIN, schemaTOpdf);

        // json - with validator
        PlainSchemaTO schemaTOjson = new PlainSchemaTO();
        schemaTOjson.setKey("BinaryJSON");
        schemaTOjson.setType(AttrSchemaType.Binary);
        schemaTOjson.setMimeType("application/json");
        schemaTOjson.setValidator("BinaryValidator");
        schemaTOjson.setAnyTypeClass("minimal user");

        createSchema(SchemaType.PLAIN, schemaTOjson);

        // json - no validator
        PlainSchemaTO schemaTOjson2 = new PlainSchemaTO();
        schemaTOjson2.setKey("BinaryJSON2");
        schemaTOjson2.setType(AttrSchemaType.Binary);
        schemaTOjson2.setMimeType("application/json");
        schemaTOjson2.setAnyTypeClass("minimal user");

        createSchema(SchemaType.PLAIN, schemaTOjson2);

        UserTO userTO = UserITCase.getUniqueSampleTO("test@syncope.apache.org");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // validation OK - application/pdf -> application/pdf
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                attrTO(attrTO("BinaryPDF",
                        Base64.getEncoder().encodeToString(
                                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.pdf"))))).
                build());

        updateUser(userPatch);
        assertNotNull(userService.read(userTO.getKey()).getPlainAttr("BinaryPDF"));

        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // validation KO - text/html -> application/pdf
        try {
            userPatch.getPlainAttrs().add(new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                    attrTO(attrTO("BinaryPDF",
                            Base64.getEncoder().encodeToString(
                                    IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.html"))))).
                    build());

            updateUser(userPatch);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }

        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // validation ok - application/json -> application/json
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                attrTO(attrTO("BinaryJSON",
                        Base64.getEncoder().encodeToString(
                                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.json"))))).
                build());

        updateUser(userPatch);
        assertNotNull(userService.read(userTO.getKey()).getPlainAttr("BinaryJSON"));

        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // no validation - application/xml -> application/json
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                attrTO(attrTO("BinaryJSON2",
                        Base64.getEncoder().encodeToString(
                                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.xml"))))).
                build());

        updateUser(userPatch);
        assertNotNull(userService.read(userTO.getKey()).getPlainAttr("BinaryJSON2"));
    }

    @Test
    public void delete() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("todelete", AttrSchemaType.String);
        schemaTO.setMandatoryCondition("false");
        createSchema(SchemaType.PLAIN, schemaTO);

        schemaService.delete(SchemaType.PLAIN, schemaTO.getKey());
        PlainSchemaTO firstname = null;
        try {
            firstname = schemaService.read(SchemaType.PLAIN, schemaTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        assertNull(firstname);
    }

    @Test
    public void list() {
        List<PlainSchemaTO> schemas = schemaService.list(new SchemaQuery.Builder().type(SchemaType.PLAIN).build());
        assertFalse(schemas.isEmpty());
        schemas.forEach(schemaTO -> {
            assertNotNull(schemaTO);
        });
    }

    @Test
    public void search() {
        List<PlainSchemaTO> schemas =
                schemaService.list(new SchemaQuery.Builder().type(SchemaType.PLAIN).keyword("fullna*").build());
        assertFalse(schemas.isEmpty());
        for (PlainSchemaTO schemaTO : schemas) {
            assertNotNull(schemaTO);
        }
    }

    @Test
    public void listByAnyTypeClass() {
        List<PlainSchemaTO> userSchemas = schemaService.list(
                new SchemaQuery.Builder().type(SchemaType.PLAIN).anyTypeClass("minimal user").build());

        assertTrue(userSchemas.stream().anyMatch(object -> "fullname".equals(object.getKey())));

        assertFalse(userSchemas.stream().anyMatch(object -> {
            return "password.cipher.algorithm".equals(object.getKey())
                    || "rderived_dx".equals(object.getKey())
                    || "icon".equals(object.getKey())
                    || "mderived_sx".equals(object.getKey())
                    || "self.membership.layout".equals(object.getKey());
        }));
    }

    @Test
    public void update() {
        PlainSchemaTO schemaTO = schemaService.read(SchemaType.PLAIN, "icon");
        assertNotNull(schemaTO);

        schemaService.update(SchemaType.PLAIN, schemaTO);
        PlainSchemaTO updatedTO = schemaService.read(SchemaType.PLAIN, "icon");
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(AttrSchemaType.Date);
        try {
            schemaService.update(SchemaType.PLAIN, updatedTO);
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

        schemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        AnyTypeClassTO typeClass = new AnyTypeClassTO();
        typeClass.setKey("issue258");
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        anyTypeClassService.create(typeClass);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue258@syncope.apache.org");
        userTO.getAuxClasses().add(typeClass.getKey());
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1.2"));

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        schemaTO.setType(AttrSchemaType.Long);
        try {
            schemaService.update(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issue259() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("schema_issue259", AttrSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        AnyTypeClassTO typeClass = new AnyTypeClassTO();
        typeClass.setKey("issue259");
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        anyTypeClassService.create(typeClass);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue259@syncope.apache.org");
        userTO.getAuxClasses().add(typeClass.getKey());
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1"));
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserTO newUserTO = SerializationUtils.clone(userTO);
        newUserTO.getMemberships().add(
                new MembershipTO.Builder().group("b1f7c12d-ec83-441f-a50e-1691daaedf3b").build());

        userTO = userService.update(newUserTO).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issue260() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("schema_issue260", AttrSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        AnyTypeClassTO typeClass = new AnyTypeClassTO();
        typeClass.setKey("issue260");
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        anyTypeClassService.create(typeClass);

        UserTO userTO = UserITCase.getUniqueSampleTO("issue260@syncope.apache.org");
        userTO.getAuxClasses().add(typeClass.getKey());
        userTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "1.2"));
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        schemaTO.setUniqueConstraint(false);
        try {
            schemaService.update(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE323() {
        PlainSchemaTO actual = schemaService.read(SchemaType.PLAIN, "icon");
        assertNotNull(actual);

        try {
            createSchema(SchemaType.PLAIN, actual);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setKey(null);
        try {
            createSchema(SchemaType.PLAIN, actual);
            fail("This should not happen");
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
            createSchema(SchemaType.PLAIN, schema);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidKey.name()));
        }
    }
}
