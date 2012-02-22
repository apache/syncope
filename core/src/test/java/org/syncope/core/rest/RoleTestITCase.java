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
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SyncopeClientExceptionType;

public class RoleTestITCase extends AbstractTest {

    @Test
    public void createWithException() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("attr1");
        attributeTO.addValue("value1");

        RoleTO newRoleTO = new RoleTO();
        newRoleTO.addAttribute(attributeTO);

        Throwable t = null;
        try {
            restTemplate.postForObject(BASE_URL + "role/create",
                    newRoleTO, RoleTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(
                    SyncopeClientExceptionType.InvalidSyncopeRole);
        }
        assertNotNull(t);
    }

    @Test
    public void create() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("lastRole");
        roleTO.setParent(8L);

        // verify inheritance password and account policies
        roleTO.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        roleTO.setAccountPolicy(6L);

        roleTO.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        roleTO.setPasswordPolicy(2L);

        AttributeTO icon = new AttributeTO();
        icon.setSchema("icon");
        icon.addValue("anIcon");

        RoleTO actual = restTemplate.postForObject(BASE_URL + "role/create",
                roleTO, RoleTO.class);

        roleTO.setId(actual.getId());

        roleTO.setPasswordPolicy(4L);

        assertEquals(roleTO, actual);

        assertNotNull(actual.getAccountPolicy());
        assertEquals(6L, (long) actual.getAccountPolicy());

        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword");
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = restTemplate.postForObject(BASE_URL + "role/create",
                roleTO, RoleTO.class);

        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL
                + "role/read/{roleId}.json", RoleTO.class, actual.getId());

        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void delete() {
        try {
            restTemplate.delete(BASE_URL + "role/delete/{roleId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        restTemplate.delete(BASE_URL + "role/delete/{roleId}", 5);
        try {
            restTemplate.getForObject(BASE_URL + "role/read/{roleId}.json",
                    RoleTO.class, 2);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<RoleTO> roleTOs = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "role/list.json", RoleTO[].class));
        assertNotNull(roleTOs);
        assertTrue(roleTOs.size() >= 8);
        for (RoleTO roleTO : roleTOs) {
            assertNotNull(roleTO);
        }
    }

    @Test
    public void parent() {
        RoleTO roleTO = restTemplate.getForObject(BASE_URL
                + "role/parent/{roleId}.json", RoleTO.class, 7);

        assertNotNull(roleTO);
        assertEquals(roleTO.getId(), 6L);
    }

    @Test
    public void read() {
        RoleTO roleTO = restTemplate.getForObject(BASE_URL
                + "role/read/{roleId}.json", RoleTO.class, 1);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());
    }

    @Test
    public void update() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("latestRole");
        roleTO.setParent(8L);

        // verify inheritance password and account policies
        roleTO.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        roleTO.setAccountPolicy(6L);

        roleTO.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        roleTO.setPasswordPolicy(2L);

        AttributeTO icon = new AttributeTO();
        icon.setSchema("icon");
        icon.addValue("anIcon");
        roleTO.addAttribute(icon);

        roleTO = restTemplate.postForObject(BASE_URL + "role/create",
                roleTO, RoleTO.class);

        assertEquals(1, roleTO.getAttributes().size());

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(Long.valueOf(6), roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(Long.valueOf(4), roleTO.getPasswordPolicy());

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("show");
        attributeMod.addValueToBeAdded("FALSE");

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setName("finalRole");
        roleMod.addAttributeToBeUpdated(attributeMod);

        // change password policy inheritance
        roleMod.setInheritPasswordPolicy(Boolean.FALSE);

        roleTO = restTemplate.postForObject(BASE_URL + "role/update",
                roleMod, RoleTO.class);

        assertEquals("finalRole", roleTO.getName());
        assertEquals(2, roleTO.getAttributes().size());

        // changes ignored because not requested (null ReferenceMod)
        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        // password policy null because not inherited
        assertNull(roleTO.getPasswordPolicy());
    }

    @Test
    public void updateRemovingVirAttribute() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("withvirtual");
        roleTO.setParent(8L);

        final AttributeTO rvirtualdata = new AttributeTO();
        rvirtualdata.setSchema("rvirtualdata");
        roleTO.addVirtualAttribute(rvirtualdata);

        roleTO = restTemplate.postForObject(
                BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirtualAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addVirtualAttributeToBeRemoved("rvirtualdata");

        roleTO = restTemplate.postForObject(
                BASE_URL + "role/update", roleMod, RoleTO.class);

        assertNotNull(roleTO);
        assertTrue(roleTO.getVirtualAttributes().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("withderived");
        roleTO.setParent(8L);

        final AttributeTO deriveddata = new AttributeTO();
        deriveddata.setSchema("rderivedschema");
        roleTO.addDerivedAttribute(deriveddata);

        roleTO = restTemplate.postForObject(
                BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerivedAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addDerivedAttributeToBeRemoved("rderivedschema");

        roleTO = restTemplate.postForObject(
                BASE_URL + "role/update", roleMod, RoleTO.class);

        assertNotNull(roleTO);
        assertTrue(roleTO.getDerivedAttributes().isEmpty());
    }

    /**
     * Role rename used to fail in case of parent null.
     *
     * http://code.google.com/p/syncope/issues/detail?id=178
     */
    @Test
    public void issue178() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("torename");

        RoleTO actual = restTemplate.postForObject(
                BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(actual);
        assertEquals("torename", actual.getName());
        assertEquals(0L, actual.getParent());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(actual.getId());
        roleMod.setName("renamed");

        actual = restTemplate.postForObject(
                BASE_URL + "role/update", roleMod, RoleTO.class);

        assertNotNull(actual);
        assertEquals("renamed", actual.getName());
        assertEquals(0L, actual.getParent());
    }
}
