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

import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.MembershipTO;
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

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttr("LOCATION"));
        assertEquals(
                anyObjectTO.getPlainAttr("location").get().getValues(),
                connObjectTO.getAttr("LOCATION").get().getValues());
    }

    @Test
    public void createInvalidMembership() {
        // 1. create anyObject in realm /odd and attempt to assign group 15, from realm /even => exception
        AnyObjectCR anyObjectCR = getSample("createInvalidMembership");
        anyObjectCR.setRealm("/odd");
        anyObjectCR.getMemberships().add(new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build());

        try {
            createAnyObject(anyObjectCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMembership, e.getType());
        }

        // 2. change anyObject's realm to /even/two, now it works
        anyObjectCR.setRealm("/even/two");

        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1"));
    }

    @Test
    public void delete() {
        try {
            anyObjectService.delete(UUID.randomUUID().toString());
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
            anyObjectService.read(deletedAnyObject.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<AnyObjectTO> anyObjectTOs = anyObjectService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).query()).
                        build());
        assertNotNull(anyObjectTOs);
        assertTrue(anyObjectTOs.getResult().size() >= 2);
        anyObjectTOs.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        AnyObjectTO anyObjectTO = anyObjectService.read("fc6dbc3a-6c07-4965-8781-921e7401a4a5");
        assertNotNull(anyObjectTO);
        assertNotNull(anyObjectTO.getPlainAttrs());
        assertFalse(anyObjectTO.getPlainAttrs().isEmpty());
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

        assertEquals(newLocation, anyObjectTO.getPlainAttr("location").get().getValues().get(0));
    }

    @Test
    public void readAttrs() {
        AnyObjectCR anyObjectCR = getSample("readAttrs");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);

        Set<Attr> attrs = anyObjectService.read(anyObjectTO.getKey(), SchemaType.PLAIN);
        assertEquals(anyObjectTO.getPlainAttrs(), attrs);

        Attr location = anyObjectService.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
        assertEquals(anyObjectTO.getPlainAttr("location").get(), location);
    }

    @Test
    public void updateAttr() {
        AnyObjectCR anyObjectCR = getSample("updateAttr");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);

        Attr updated = attr("location", "newlocation");
        anyObjectService.update(anyObjectTO.getKey(), SchemaType.PLAIN, updated);

        Attr location = anyObjectService.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
        assertEquals(updated, location);
    }

    @Test
    public void deleteAttr() {
        AnyObjectCR anyObjectCR = getSample("deleteAttr");
        AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
        assertNotNull(anyObjectTO);
        assertNotNull(anyObjectTO.getPlainAttr("location"));

        anyObjectService.delete(anyObjectTO.getKey(), SchemaType.PLAIN, "location");

        try {
            anyObjectService.read(anyObjectTO.getKey(), SchemaType.PLAIN, "location");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE756() {
        AnyObjectCR anyObjectCR = getSample("issueSYNCOPE756");
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder().otherEnd(
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
        AnyObjectUR anyObjectPatch = new AnyObjectUR();
        anyObjectPatch.setKey("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        anyObjectPatch.getResources().add(new StringPatchItem.Builder().value(RESOURCE_NAME_DBSCRIPTED).build());
        anyObjectPatch.getAuxClasses().add(new StringPatchItem.Builder().value("csv").build());

        for (int i = 0; i < 2; i++) {
            updateAnyObject(anyObjectPatch);
        }

        // 2. remove resources and auxiliary classes
        anyObjectPatch.getResources().clear();
        anyObjectPatch.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_DBSCRIPTED)
                .operation(PatchOperation.DELETE)
                .build());
        anyObjectPatch.getAuxClasses().clear();
        anyObjectPatch.getAuxClasses().add(new StringPatchItem.Builder()
                .value("csv")
                .operation(PatchOperation.DELETE)
                .build());

        updateAnyObject(anyObjectPatch);

        AnyObjectTO printer = anyObjectService.read("8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertFalse(printer.getResources().contains(RESOURCE_NAME_DBSCRIPTED), "Should not contain removed resources");
        assertFalse(printer.getAuxClasses().contains("csv"), "Should not contain removed auxiliary classes");
    }
}
