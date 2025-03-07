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

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlainSchemaITCase extends AbstractITCase {

    private static PlainSchemaTO buildPlainSchemaTO(final String name, final AttrSchemaType type) {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(name + getUUIDString());
        schemaTO.setType(type);
        return schemaTO;
    }

    @Test
    public void create() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("testAttribute", AttrSchemaType.String);
        schemaTO.setMandatoryCondition("false");
        schemaTO.getLabels().put(Locale.ENGLISH, "Test");
        schemaTO.getLabels().put(Locale.ITALIAN, "Prova");

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
    public void createEnumWithoutValues() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("enumcheck");
        schemaTO.setType(AttrSchemaType.Enum);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidSchema.name()));
        }
    }

    @Test
    public void createDropdownWithoutProvider() {
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("dropdowncheck");
        schemaTO.setType(AttrSchemaType.Dropdown);

        try {
            createSchema(SchemaType.PLAIN, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidSchema.name()));
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
    public void binaryValidation() throws IOException {
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

        UserTO userTO = createUser(UserITCase.getUniqueSample("test@syncope.apache.org")).getEntity();
        try {
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            // validation OK - application/pdf -> application/pdf
            userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("BinaryPDF",
                    Base64.getEncoder().encodeToString(
                            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.pdf"))))).
                    operation(PatchOperation.ADD_REPLACE).
                    build());

            updateUser(userUR);
            assertNotNull(USER_SERVICE.read(userTO.getKey()).getPlainAttr("BinaryPDF"));

            userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            // validation KO - text/html -> application/pdf
            try {
                userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("BinaryPDF",
                        Base64.getEncoder().encodeToString(
                                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.html"))))).
                        operation(PatchOperation.ADD_REPLACE).
                        build());

                updateUser(userUR);
                fail("This should not be reachable");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidValues, e.getType());
            }

            userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            // validation ok - application/json -> application/json
            userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("BinaryJSON",
                    Base64.getEncoder().encodeToString(
                            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.json"))))).
                    operation(PatchOperation.ADD_REPLACE).
                    build());

            updateUser(userUR);
            assertNotNull(USER_SERVICE.read(userTO.getKey()).getPlainAttr("BinaryJSON"));

            userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            // no validation - application/xml -> application/json
            userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("BinaryJSON2",
                    Base64.getEncoder().encodeToString(
                            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/test.xml"))))).
                    operation(PatchOperation.ADD_REPLACE).
                    build());

            updateUser(userUR);
            assertNotNull(USER_SERVICE.read(userTO.getKey()).getPlainAttr("BinaryJSON2"));
        } finally {
            USER_SERVICE.delete(userTO.getKey());

            SCHEMA_SERVICE.delete(SchemaType.PLAIN, schemaTOpdf.getKey());
            SCHEMA_SERVICE.delete(SchemaType.PLAIN, schemaTOjson.getKey());
            SCHEMA_SERVICE.delete(SchemaType.PLAIN, schemaTOjson2.getKey());
        }
    }

    @Test
    public void delete() {
        PlainSchemaTO schemaTO = buildPlainSchemaTO("todelete", AttrSchemaType.String);
        schemaTO.setMandatoryCondition("false");
        createSchema(SchemaType.PLAIN, schemaTO);

        SCHEMA_SERVICE.delete(SchemaType.PLAIN, schemaTO.getKey());
        PlainSchemaTO firstname = null;
        try {
            firstname = SCHEMA_SERVICE.read(SchemaType.PLAIN, schemaTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        assertNull(firstname);
    }

    @Test
    public void search() {
        List<PlainSchemaTO> schemas = SCHEMA_SERVICE.search(new SchemaQuery.Builder().type(SchemaType.PLAIN).build());
        assertFalse(schemas.isEmpty());
        schemas.forEach(Assertions::assertNotNull);

        schemas = SCHEMA_SERVICE.search(new SchemaQuery.Builder().type(SchemaType.PLAIN).keyword("fullna*").build());
        assertFalse(schemas.isEmpty());
        schemas.forEach(Assertions::assertNotNull);
    }

    @Test
    public void searchByAnyTypeClass() {
        List<PlainSchemaTO> userSchemas = SCHEMA_SERVICE.search(
                new SchemaQuery.Builder().type(SchemaType.PLAIN).anyTypeClass("minimal user").build());

        assertTrue(userSchemas.stream().anyMatch(object -> "fullname".equals(object.getKey())));

        assertFalse(userSchemas.stream().anyMatch(object -> "password.cipher.algorithm".equals(object.getKey())
                || "rderived_dx".equals(object.getKey())
                || "icon".equals(object.getKey())
                || "mderived_sx".equals(object.getKey())
                || "self.membership.layout".equals(object.getKey())));
    }

    @Test
    public void update() {
        PlainSchemaTO schemaTO = SCHEMA_SERVICE.read(SchemaType.PLAIN, "icon");
        assertNotNull(schemaTO);

        SCHEMA_SERVICE.update(SchemaType.PLAIN, schemaTO);
        PlainSchemaTO updatedTO = SCHEMA_SERVICE.read(SchemaType.PLAIN, "icon");
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(AttrSchemaType.Date);
        try {
            SCHEMA_SERVICE.update(SchemaType.PLAIN, updatedTO);
            fail("This should not be reachable");
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
        ANY_TYPE_CLASS_SERVICE.create(typeClass);

        UserCR userCR = UserITCase.getUniqueSample("issue258@syncope.apache.org");
        userCR.getAuxClasses().add(typeClass.getKey());
        userCR.getPlainAttrs().add(attr(schemaTO.getKey(), "1.2"));

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        schemaTO.setType(AttrSchemaType.Long);
        try {
            SCHEMA_SERVICE.update(SchemaType.PLAIN, schemaTO);
            fail("This should not be reachable");
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
        ANY_TYPE_CLASS_SERVICE.create(typeClass);

        UserCR userCR = UserITCase.getUniqueSample("issue259@syncope.apache.org");
        userCR.getAuxClasses().add(typeClass.getKey());
        userCR.getPlainAttrs().add(attr(schemaTO.getKey(), "1"));
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR req = new UserUR.Builder(userTO.getKey()).
                membership(new MembershipUR.Builder("b1f7c12d-ec83-441f-a50e-1691daaedf3b").build()).build();

        UserTO newUserTO = updateUser(req).getEntity();
        assertNotNull(newUserTO);
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
        ANY_TYPE_CLASS_SERVICE.create(typeClass);

        UserCR userCR = UserITCase.getUniqueSample("issue260@syncope.apache.org");
        userCR.getAuxClasses().add(typeClass.getKey());
        userCR.getPlainAttrs().add(attr(schemaTO.getKey(), "1.2"));
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        schemaTO.setUniqueConstraint(false);
        try {
            SCHEMA_SERVICE.update(SchemaType.PLAIN, schemaTO);
            fail("This should not be reachable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE323() {
        PlainSchemaTO actual = SCHEMA_SERVICE.read(SchemaType.PLAIN, "icon");
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
