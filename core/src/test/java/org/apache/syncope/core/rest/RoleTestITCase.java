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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.client.mod.AttributeMod;
import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.ConnObjectTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

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
            restTemplate.postForObject(BASE_URL + "role/create", newRoleTO, RoleTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(SyncopeClientExceptionType.InvalidSyncopeRole);
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
        roleTO.addAttribute(icon);

        AttributeTO ownerDN = new AttributeTO();
        ownerDN.setSchema("ownerDN");
        roleTO.addDerivedAttribute(ownerDN);

        AttributeTO rvirtualdata = new AttributeTO();
        rvirtualdata.setSchema("rvirtualdata");
        rvirtualdata.addValue("rvirtualvalue");
        roleTO.addVirtualAttribute(rvirtualdata);

        roleTO.setRoleOwner(8L);

        roleTO.addResource("resource-ldap");

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);
        assertNotNull(roleTO);

        assertNotNull(roleTO.getVirtualAttributeMap());
        assertNotNull(roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues());
        assertFalse(roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues().get(0));

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        assertTrue(roleTO.getResources().contains("resource-ldap"));

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/resource-ldap/read/ROLE/lastRole.json", ConnObjectTO.class);
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttributeMap().get("owner"));
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword");
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);
        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL + "role/read/{roleId}.json", RoleTO.class, actual.getId());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void delete() {
        try {
            restTemplate.getForObject(BASE_URL + "role/delete/{roleId}", RoleTO.class, 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        RoleTO roleTO = new RoleTO();
        roleTO.setName("toBeDeleted");
        roleTO.setParent(8L);

        roleTO.addResource("resource-ldap");

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);
        assertNotNull(roleTO);

        RoleTO deletedRole = restTemplate.getForObject(BASE_URL + "role/delete/{roleId}", RoleTO.class, roleTO.getId());
        assertNotNull(deletedRole);

        try {
            restTemplate.getForObject(BASE_URL + "role/read/{roleId}.json", RoleTO.class, deletedRole.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<RoleTO> roleTOs = Arrays.asList(restTemplate.getForObject(BASE_URL + "role/list.json", RoleTO[].class));
        assertNotNull(roleTOs);
        assertTrue(roleTOs.size() >= 8);
        for (RoleTO roleTO : roleTOs) {
            assertNotNull(roleTO);
        }
    }

    @Test
    public void parent() {
        RoleTO roleTO = restTemplate.getForObject(BASE_URL + "role/parent/{roleId}.json", RoleTO.class, 7);

        assertNotNull(roleTO);
        assertEquals(roleTO.getId(), 6L);
    }

    @Test
    public void read() {
        RoleTO roleTO = restTemplate.getForObject(BASE_URL + "role/read/{roleId}.json", RoleTO.class, 1);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}", UserTO.class, 1);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        PreemptiveAuthHttpRequestFactory requestFactory =
                (PreemptiveAuthHttpRequestFactory) restTemplate.getRequestFactory();
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user1", "password"));

        SyncopeClientException exception = null;
        try {
            restTemplate.getForObject(BASE_URL + "role/selfRead/{roleId}", RoleTO.class, 3);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        RoleTO roleTO = restTemplate.getForObject(BASE_URL + "role/selfRead/{roleId}", RoleTO.class, 1);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());

        // restore admin authentication
        super.resetRestTemplate();
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

        roleTO.addResource("resource-ldap");

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);

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

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);

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

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirtualAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addVirtualAttributeToBeRemoved("rvirtualdata");

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);

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

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerivedAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addDerivedAttributeToBeRemoved("rderivedschema");

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);

        assertNotNull(roleTO);
        assertTrue(roleTO.getDerivedAttributes().isEmpty());
    }

    @Test
    public void updateAsRoleOwner() {
        // 1. read role as admin
        RoleTO roleTO = restTemplate.getForObject(BASE_URL + "role/read/{roleId}.json", RoleTO.class, 7);

        // 2. prepare update
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setName("Managing Director");

        // 3. try to update as user3, not owner of role 7 - fail
        PreemptiveAuthHttpRequestFactory requestFactory =
                (PreemptiveAuthHttpRequestFactory) restTemplate.getRequestFactory();
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user2", "password"));

        try {
            restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }

        // 4. update as user5, owner of role 7 because owner of role 6 with inheritance - success
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user5", "password"));

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);
        assertEquals("Managing Director", roleTO.getName());

        // restore admin authentication
        super.resetRestTemplate();
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

        RoleTO actual = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);

        assertNotNull(actual);
        assertEquals("torename", actual.getName());
        assertEquals(0L, actual.getParent());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(actual.getId());
        roleMod.setName("renamed");

        actual = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);

        assertNotNull(actual);
        assertEquals("renamed", actual.getName());
        assertEquals(0L, actual.getParent());
    }

    @Test
    public void issueSYNCOPE228() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("issueSYNCOPE228");
        roleTO.setParent(8L);
        roleTO.setInheritAccountPolicy(false);
        roleTO.setAccountPolicy(6L);
        roleTO.setInheritPasswordPolicy(true);
        roleTO.setPasswordPolicy(2L);

        AttributeTO icon = new AttributeTO();
        icon.setSchema("icon");
        icon.addValue("anIcon");
        roleTO.addAttribute(icon);

        roleTO.addEntitlement("USER_READ");
        roleTO.addEntitlement("SCHEMA_READ");

        roleTO = restTemplate.postForObject(BASE_URL + "role/create", roleTO, RoleTO.class);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertFalse(roleTO.getEntitlements().isEmpty());

        List<String> entitlements = roleTO.getEntitlements();

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setInheritDerivedAttributes(Boolean.TRUE);

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);
        assertNotNull(roleTO);
        assertEquals(entitlements, roleTO.getEntitlements());

        roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setEntitlements(new ArrayList<String>());

        roleTO = restTemplate.postForObject(BASE_URL + "role/update", roleMod, RoleTO.class);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().isEmpty());
    }
}
