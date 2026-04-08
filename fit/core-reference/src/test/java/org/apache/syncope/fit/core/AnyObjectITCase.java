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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnyObjectITCase extends AbstractITCase {

    public static AnyObjectCR getSample(final String location) {
        return new AnyObjectCR.Builder(SyncopeConstants.ROOT_REALM, PRINTER, location + getUUIDString()).
                plainAttr(attr("location", location + getUUIDString())).
                resource(RESOURCE_NAME_DBSCRIPTED).
                build();
    }

    @Test
    public void create() {
        AnyObjectCR anyObjectCR = getSample("create");

        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);
        assertEquals("REST", anyObjectTO.getCreationContext());
        assertEquals("REST", anyObjectTO.getLastChangeContext());

        ConnObject connObjectTO =
                RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttr("LOCATION"));
        assertEquals(
                anyObjectTO.getPlainAttr("location").get().getValues(),
                connObjectTO.getAttr("LOCATION").get().getValues());
    }

    @Test
    public void delete() {
        try {
            ANY_OBJECT_SERVICE.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        AnyObjectCR anyObjectCR = getSample("deletable");
        anyObjectCR.setRealm("/even");

        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);

        AnyObjectTO deletedAnyObject = deleteAnyObject(anyObjectTO.getKey()).getEntity();
        assertNotNull(deletedAnyObject);

        try {
            ANY_OBJECT_SERVICE.read(deletedAnyObject.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<AnyObjectTO> anyObjectTOs = ANY_OBJECT_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).query()).
                        build());
        assertNotNull(anyObjectTOs);
        assertTrue(anyObjectTOs.getResult().size() >= 2);
        anyObjectTOs.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        AnyObjectTO anyObjectTO = ANY_OBJECT_SERVICE.read("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObjectTO);
        assertNotNull(anyObjectTO.getPlainAttrs());
        assertFalse(anyObjectTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void readByName() {
        AnyObjectTO anyObjectTO = ANY_OBJECT_SERVICE.read(PRINTER, "HP LJ 1300n");
        assertEquals("fc6dbc3a-6c07-4965-8781-921e7401a4a5", anyObjectTO.getKey());
    }

    @Test
    public void update() {
        AnyObjectCR anyObjectCR = getSample("update");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();

        assertEquals(1, anyObjectTO.getPlainAttrs().size());

        AnyObjectUR anyObjectUR = new AnyObjectUR();
        anyObjectUR.setKey(anyObjectTO.getKey());
        String newLocation = "new" + getUUIDString();
        anyObjectUR.getPlainAttrs().add(attrAddReplacePatch("location", newLocation));

        anyObjectTO = updateAnyObject(anyObjectUR).getEntity();

        assertEquals(newLocation, anyObjectTO.getPlainAttr("location").get().getValues().getFirst());
    }

    @Test
    public void readAttrs() {
        AnyObjectCR anyObjectCR = getSample("readAttrs");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);

        Set<Attr> attrs = ANY_OBJECT_SERVICE.read(anyObjectTO.getKey(), SchemaType.PLAIN);
        assertEquals(anyObjectTO.getPlainAttrs(), attrs);

        Attr location = ANY_OBJECT_SERVICE.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
        assertEquals(anyObjectTO.getPlainAttr("location").get(), location);
    }

    @Test
    public void updateAttr() {
        AnyObjectCR anyObjectCR = getSample("updateAttr");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);

        Attr updated = attr("location", "newlocation");
        ANY_OBJECT_SERVICE.update(anyObjectTO.getKey(), SchemaType.PLAIN, updated);

        Attr location = ANY_OBJECT_SERVICE.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
        assertEquals(updated, location);
    }

    @Test
    public void deleteAttr() {
        AnyObjectCR anyObjectCR = getSample("deleteAttr");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);
        assertNotNull(anyObjectTO.getPlainAttr("location"));

        ANY_OBJECT_SERVICE.delete(anyObjectTO.getKey(), SchemaType.PLAIN, "location");

        try {
            ANY_OBJECT_SERVICE.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE1472() {
        // 1. assign resource-db-scripted again to Canon MF 8030cn and update twice
        AnyObjectUR updateReq = new AnyObjectUR();
        updateReq.setKey("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        updateReq.getResources().add(new StringPatchItem.Builder().value(RESOURCE_NAME_DBSCRIPTED).build());
        updateReq.getAuxClasses().add(new StringPatchItem.Builder().value("csv").build());

        for (int i = 0; i < 2; i++) {
            updateAnyObject(updateReq);
        }

        // 2. remove resources and auxiliary classes
        updateReq.getResources().clear();
        updateReq.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_DBSCRIPTED)
                .operation(PatchOperation.DELETE)
                .build());
        updateReq.getAuxClasses().clear();
        updateReq.getAuxClasses().add(new StringPatchItem.Builder()
                .value("csv")
                .operation(PatchOperation.DELETE)
                .build());

        updateAnyObject(updateReq);

        AnyObjectTO printer = ANY_OBJECT_SERVICE.read("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertFalse(printer.getResources().contains(RESOURCE_NAME_DBSCRIPTED), "Should not contain removed resources");
        assertFalse(printer.getAuxClasses().contains("csv"), "Should not contain removed auxiliary classes");
    }

    @Test
    public void issueSYNCOPE1957() {
        // prepare
        PlainSchemaTO schema1 = new PlainSchemaTO();
        schema1.setKey("schema1" + getUUIDString());
        schema1.setType(AttrSchemaType.Boolean);
        createSchema(SchemaType.PLAIN, schema1);

        PlainSchemaTO schema2 = new PlainSchemaTO();
        schema2.setKey("schema2" + getUUIDString());
        schema2.setType(AttrSchemaType.Boolean);
        createSchema(SchemaType.PLAIN, schema2);

        String class1Key = "class1" + getUUIDString();
        AnyTypeClassTO class1 = new AnyTypeClassTO();
        class1.setKey(class1Key);
        class1.getPlainSchemas().add(schema1.getKey());
        class1.getPlainSchemas().add(schema2.getKey());

        Response response = ANY_TYPE_CLASS_SERVICE.create(class1);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        class1 = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

        // 1. create user, group and printer with auxClass class1
        Consumer<AnyCR> setupAnyCR = anyCR -> {
            anyCR.getResources().clear();
            anyCR.getAuxClasses().add(class1Key);
            anyCR.getPlainAttrs().add(attr(schema1.getKey(), "true"));
            anyCR.getPlainAttrs().add(attr(schema2.getKey(), "true"));
        };
        Consumer<AnyTO> checkAnyTO = anyTO -> {
            assertTrue(anyTO.getPlainAttr(schema1.getKey()).isPresent());
            assertTrue(anyTO.getPlainAttr(schema2.getKey()).isPresent());
        };

        UserCR userCR = UserITCase.getUniqueSample("syncope1957@apache.org");
        setupAnyCR.accept(userCR);

        UserTO user = createUser(userCR).getEntity();
        checkAnyTO.accept(user);

        GroupCR groupCR = GroupITCase.getSample("syncope1957");
        setupAnyCR.accept(groupCR);

        GroupTO group = createGroup(groupCR).getEntity();
        checkAnyTO.accept(group);

        AnyObjectCR printerCR = getSample("syncope1957");
        setupAnyCR.accept(printerCR);

        AnyObjectTO printer = createAnyObject(printerCR).getEntity();
        checkAnyTO.accept(printer);

        // 2. create new anytypeclass and move schema there
        class1.getPlainSchemas().remove(schema2.getKey());
        ANY_TYPE_CLASS_SERVICE.update(class1);

        class1 = ANY_TYPE_CLASS_SERVICE.read(class1.getKey());
        assertEquals(List.of(schema1.getKey()), class1.getPlainSchemas());

        String class2Key = "class2" + getUUIDString();
        AnyTypeClassTO class2 = new AnyTypeClassTO();
        class2.setKey(class2Key);
        class2.getPlainSchemas().add(schema2.getKey());

        response = ANY_TYPE_CLASS_SERVICE.create(class2);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        class2 = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertEquals(List.of(schema2.getKey()), class2.getPlainSchemas());

        // 3. update user, group and printer by adding auxClass class2
        Consumer<AnyUR> setupAnyUR = anyUR -> anyUR.getAuxClasses().
                add(new StringPatchItem.Builder().value(class2Key).build());

        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());
        setupAnyUR.accept(userUR);

        user = updateUser(userUR).getEntity();
        checkAnyTO.accept(user);

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(group.getKey());
        setupAnyUR.accept(groupUR);

        group = updateGroup(groupUR).getEntity();
        checkAnyTO.accept(group);

        AnyObjectUR printerUR = new AnyObjectUR();
        printerUR.setKey(printer.getKey());
        setupAnyUR.accept(printerUR);

        printer = updateAnyObject(printerUR).getEntity();
        checkAnyTO.accept(printer);
    }

    @Test
    public void issueSYNCOPE1960() {
        // 1. create schema and class
        PlainSchemaTO plainSchema = new PlainSchemaTO();
        plainSchema.setKey("new_plain_schema" + getUUIDString());
        plainSchema.setType(AttrSchemaType.String);
        plainSchema = createSchema(SchemaType.PLAIN, plainSchema);

        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getPlainSchemas().add(plainSchema.getKey());
        ANY_TYPE_CLASS_SERVICE.create(newClass);

        // 2. create printer with the new class as aux class
        AnyObjectCR anyObjectCR = getSample("syncope1960");
        anyObjectCR.getResources().clear();
        anyObjectCR.getAuxClasses().add(newClass.getKey());
        anyObjectCR.getPlainAttrs().add(attr(plainSchema.getKey(), "value"));

        AnyObjectTO printer = createAnyObject(anyObjectCR).getEntity();
        assertTrue(printer.getPlainAttr(plainSchema.getKey()).isPresent());
        assertTrue(printer.getAuxClasses().contains(newClass.getKey()));

        // 3. remove aux class
        AnyObjectUR anyObjectUR = new AnyObjectUR.Builder(printer.getKey()).
                auxClass(new StringPatchItem.Builder().value(newClass.getKey()).
                        operation(PatchOperation.DELETE).build()).
                build();

        printer = updateAnyObject(anyObjectUR).getEntity();
        assertTrue(printer.getPlainAttr(plainSchema.getKey()).isEmpty());
        assertFalse(printer.getAuxClasses().contains(newClass.getKey()));
    }
}
