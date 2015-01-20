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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.SchemaType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void create() {
        SchemaTO testKey = new SchemaTO();
        testKey.setName("testKey");
        testKey.setType(AttributeSchemaType.String);
        createSchema(AttributableType.CONFIGURATION, SchemaType.NORMAL, testKey);

        AttributeTO conf = new AttributeTO();
        conf.setSchema("testKey");
        conf.getValues().add("testValue");

        configurationService.set(conf.getSchema(), conf);

        AttributeTO actual = configurationService.read(conf.getSchema());
        assertEquals(actual, conf);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {
        try {
            configurationService.delete("nonExistent");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        AttributeTO tokenLength = configurationService.read("token.length");

        configurationService.delete("token.length");
        try {
            configurationService.read("token.length");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        configurationService.set(tokenLength.getSchema(), tokenLength);

        AttributeTO actual = configurationService.read(tokenLength.getSchema());
        assertEquals(actual, tokenLength);
    }

    @Test
    public void list() {
        ConfTO wholeConf = configurationService.list();
        assertNotNull(wholeConf);
        for (AttributeTO conf : wholeConf.getAttrs()) {
            assertNotNull(conf);
        }
    }

    @Test
    public void read() {
        AttributeTO conf = configurationService.read("token.expireTime");
        assertNotNull(conf);
    }

    @Test
    public void update() {
        AttributeTO expireTime = configurationService.read("token.expireTime");
        int value = Integer.parseInt(expireTime.getValues().get(0));
        value++;
        expireTime.getValues().set(0, value + "");

        configurationService.set(expireTime.getSchema(), expireTime);

        AttributeTO newConfigurationTO = configurationService.read(expireTime.getSchema());
        assertEquals(expireTime, newConfigurationTO);
    }

    @Test
    public void dbExport() throws IOException {
        Response response = configurationService.export();
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_XML));
        String contentDisposition = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        String configExport = IOUtils.toString((InputStream) entity, SyncopeConstants.DEFAULT_ENCODING);
        assertFalse(configExport.isEmpty());
        assertTrue(configExport.length() > 1000);
    }

    @Test
    public void issueSYNCOPE418() {
        SchemaTO failing = new SchemaTO();
        failing.setName("http://schemas.examples.org/security/authorization/organizationUnit");
        failing.setType(AttributeSchemaType.String);

        try {
            createSchema(AttributableType.CONFIGURATION, SchemaType.NORMAL, failing);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidCSchema, e.getType());

            assertNotNull(e.getElements());
            assertEquals(1, e.getElements().size());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidName.name()));
        }
    }
    
    @Test
    public void issueSYNCOPE629() throws IOException{
        SchemaTO membershipKey = new SchemaTO();
        membershipKey.setName("membershipKey"+getUUIDString());
        membershipKey.setType(AttributeSchemaType.String);
        createSchema(AttributableType.MEMBERSHIP, SchemaType.NORMAL, membershipKey);
        
        SchemaTO roleKey = new SchemaTO();
        roleKey.setName("roleKey"+getUUIDString());
        roleKey.setType(AttributeSchemaType.String);
        createSchema(AttributableType.ROLE, SchemaType.NORMAL, roleKey);        
                
        RoleTO roleTO = new RoleTO();
        roleTO.setName("aRole" + getUUIDString());
        roleTO.setParent(8L);        
        // verify inheritance password and account policies
        roleTO.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        roleTO.setAccountPolicy(6L);
        roleTO.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        roleTO.setPasswordPolicy(2L);
        roleTO.getRAttrTemplates().add("icon");
        roleTO.getAttrs().add(attributeTO("icon", "anIcon"));
        roleTO.getResources().add(RESOURCE_NAME_LDAP);       
        roleTO.getMAttrTemplates().add(membershipKey.getName());
        roleTO.getRAttrTemplates().add(roleKey.getName());
        RoleTO testRole = createRole(roleTO);
                       
        Response response = configurationService.export();
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_XML));
        String contentDisposition = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        String configExport = IOUtils.toString((InputStream) entity, SyncopeConstants.DEFAULT_ENCODING);
        assertFalse(configExport.isEmpty());
        assertTrue(configExport.length() > 1000);
        
        String[] result = StringUtils.substringsBetween(configExport, "<RATTRTEMPLATE", "/>");
        boolean rattrExists = false;
        for(String entry : result){
            if(entry.contains(roleKey.getName())) rattrExists = true;
        }
        assertTrue(rattrExists);
        
        result = StringUtils.substringsBetween(configExport, "<MATTRTEMPLATE", "/>");
        boolean mattrExists = false;
        for(String entry : result){
            if(entry.contains(membershipKey.getName())) mattrExists = true;
        }
        assertTrue(mattrExists);
        
        deleteRole(testRole.getId());

    }
}
