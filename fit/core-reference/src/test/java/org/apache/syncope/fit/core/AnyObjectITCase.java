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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
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
    public void unlimitedRelationships() {
        AnyObjectCR anyObjectCR = getSample("unlimited1");
        anyObjectCR.setRealm("/even/two");
        anyObjectCR.getResources().clear();
        AnyObjectTO left = createAnyObject(anyObjectCR).getEntity();

        anyObjectCR = getSample("unlimited2");
        anyObjectCR.setRealm(SyncopeConstants.ROOT_REALM);
        anyObjectCR.getResources().clear();
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder("inclusion").
                otherEnd(left.getType(), left.getKey()).build());
        AnyObjectTO right = createAnyObject(anyObjectCR).getEntity();

        assertEquals(1, right.getRelationships().size());
        assertEquals(left.getKey(), right.getRelationships().getFirst().getOtherEndKey());

        AnyObjectUR anyObjectUR = new AnyObjectUR.Builder(left.getKey()).
                relationship(new RelationshipUR.Builder(new RelationshipTO.Builder("inclusion").
                        otherEnd(right.getType(), right.getKey()).build()).build()).build();
        left = updateAnyObject(anyObjectUR).getEntity();
        assertEquals(2, left.getRelationships().size());
        assertTrue(left.getRelationships().stream().anyMatch(r -> right.getKey().equals(r.getOtherEndKey())));
    }

    @Test
    public void issueSYNCOPE756() {
        AnyObjectCR anyObjectCR = getSample("issueSYNCOPE756");
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder("neighborhood").otherEnd(
                AnyTypeKind.USER.name(), "1417acbe-cbf6-4277-9372-e75e04f97000").build());

        try {
            createAnyObject(anyObjectCR).getEntity();
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidAnyType, e.getType());
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
    public void issueSYNCOPE1686() {
        // Create printers
        AnyObjectCR printer1CR = getSample("printer1");
        printer1CR.getResources().clear();
        String key1 = createAnyObject(printer1CR).getEntity().getKey();

        AnyObjectCR printer2CR = getSample("printer2");
        printer2CR.getResources().clear();
        String key2 = createAnyObject(printer2CR).getEntity().getKey();

        AnyObjectCR printer3CR = getSample("printer3");
        printer3CR.getResources().clear();
        String key3 = createAnyObject(printer3CR).getEntity().getKey();

        // Add relationships: printer1 -> printer2 and printer2 -> printer3
        AnyObjectUR relationship1To2 = new AnyObjectUR.Builder(key1)
                .relationship(new RelationshipUR.Builder(
                        new RelationshipTO.Builder("inclusion").otherEnd(PRINTER, key2).build()).build())
                .build();
        AnyObjectUR relationship2To3 = new AnyObjectUR.Builder(key2)
                .relationship(new RelationshipUR.Builder(
                        new RelationshipTO.Builder("inclusion").otherEnd(PRINTER, key3).build()).build())
                .build();

        updateAnyObject(relationship1To2);
        updateAnyObject(relationship2To3);

        // Read updated printers
        AnyObjectTO printer1 = ANY_OBJECT_SERVICE.read(key1);
        AnyObjectTO printer2 = ANY_OBJECT_SERVICE.read(key2);
        AnyObjectTO printer3 = ANY_OBJECT_SERVICE.read(key3);

        // Verify relationships for printer1
        assertEquals(1, printer1.getRelationships().size());
        RelationshipTO rel1 = printer1.getRelationships().getFirst();
        assertEquals(RelationshipTO.End.LEFT, rel1.getEnd());
        assertEquals(printer2.getKey(), rel1.getOtherEndKey());
        assertEquals(printer2.getType(), rel1.getOtherEndType());
        assertEquals(printer2.getName(), rel1.getOtherEndName());

        // Verify relationships for printer2
        assertEquals(2, printer2.getRelationships().size());
        assertTrue(printer2.getRelationships().stream()
                .anyMatch(r -> r.getEnd() == RelationshipTO.End.LEFT
                && printer3.getKey().equals(r.getOtherEndKey())
                && printer3.getType().equals(r.getOtherEndType())
                && printer3.getName().equals(r.getOtherEndName())));
        assertTrue(printer2.getRelationships().stream()
                .anyMatch(r -> r.getEnd() == RelationshipTO.End.RIGHT
                && printer1.getKey().equals(r.getOtherEndKey())
                && printer1.getType().equals(r.getOtherEndType())
                && printer1.getName().equals(r.getOtherEndName())));

        // Verify relationships for printer3
        assertEquals(1, printer3.getRelationships().size());
        RelationshipTO rel3 = printer3.getRelationships().getFirst();
        assertEquals(RelationshipTO.End.RIGHT, rel3.getEnd());
        assertEquals(printer2.getKey(), rel3.getOtherEndKey());
        assertEquals(printer2.getType(), rel3.getOtherEndType());
        assertEquals(printer2.getName(), rel3.getOtherEndName());

        // Test invalid relationship with End.RIGHT
        AnyObjectCR printer4CR = getSample("printer4");
        printer4CR.getResources().clear();
        printer4CR.getRelationships().add(
                new RelationshipTO.Builder("inclusion", RelationshipTO.End.RIGHT).otherEnd(PRINTER, key1).build());

        SyncopeClientException e = assertThrows(SyncopeClientException.class, () -> createAnyObject(printer4CR));
        assertEquals(ClientExceptionType.InvalidRelationship, e.getType());
        assertTrue(e.getMessage().contains("Relationships shall be created or updated only from their left end"));
    }
}
