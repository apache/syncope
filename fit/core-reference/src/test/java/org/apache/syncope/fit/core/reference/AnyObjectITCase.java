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
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AnyObjectITCase extends AbstractITCase {

    public static AnyObjectTO getSampleTO(final String location) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setRealm(SyncopeConstants.ROOT_REALM);
        anyObjectTO.setType("PRINTER");
        anyObjectTO.getPlainAttrs().add(attrTO("location", location + getUUIDString()));

        anyObjectTO.getResources().add(RESOURCE_NAME_DBSCRIPTED);
        return anyObjectTO;
    }

    @Test
    public void create() {
        AnyObjectTO anyObjectTO = getSampleTO("create");

        anyObjectTO = createAnyObject(anyObjectTO);
        assertNotNull(anyObjectTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getPlainAttrMap().get("location"));
        assertEquals(anyObjectTO.getPlainAttrMap().get("location"), connObjectTO.getPlainAttrMap().get("location"));
    }

    @Test
    public void delete() {
        try {
            anyObjectService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        AnyObjectTO anyObjectTO = getSampleTO("deletable");
        anyObjectTO.setRealm("/even");

        anyObjectTO = createAnyObject(anyObjectTO);
        assertNotNull(anyObjectTO);

        AnyObjectTO deletedAnyObject = deleteAnyObject(anyObjectTO.getKey());
        assertNotNull(deletedAnyObject);

        try {
            anyObjectService.read(deletedAnyObject.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<AnyObjectTO> anyObjectTOs = anyObjectService.list("PRINTER", SyncopeClient.getAnyListQueryBuilder().
                realm(SyncopeConstants.ROOT_REALM).build());
        assertNotNull(anyObjectTOs);
        assertTrue(anyObjectTOs.getResult().size() >= 2);
        for (AnyObjectTO anyObjectTO : anyObjectTOs.getResult()) {
            assertNotNull(anyObjectTO);
        }
    }

    @Test
    public void read() {
        AnyObjectTO anyObjectTO = anyObjectService.read(1L);

        assertNotNull(anyObjectTO);
        assertNotNull(anyObjectTO.getPlainAttrs());
        assertFalse(anyObjectTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void update() {
        AnyObjectTO anyObjectTO = getSampleTO("update");
        anyObjectTO = createAnyObject(anyObjectTO);

        assertEquals(1, anyObjectTO.getPlainAttrs().size());

        AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(anyObjectTO.getKey());
        String newLocation = "new" + getUUIDString();
        anyObjectMod.getPlainAttrsToUpdate().add(attrMod("location", newLocation));

        anyObjectTO = updateAnyObject(anyObjectMod);

        assertEquals(newLocation, anyObjectTO.getPlainAttrMap().get("location").getValues().get(0));
    }

}
