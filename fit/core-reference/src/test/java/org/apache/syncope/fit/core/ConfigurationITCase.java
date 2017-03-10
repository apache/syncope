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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class ConfigurationITCase extends AbstractITCase {

    @Test
    public void create() {
        PlainSchemaTO testKey = new PlainSchemaTO();
        testKey.setKey("testKey" + getUUIDString());
        testKey.setType(AttrSchemaType.String);
        createSchema(SchemaType.PLAIN, testKey);

        AttrTO conf = new AttrTO.Builder().schema(testKey.getKey()).value("testValue").build();

        configurationService.set(conf);

        AttrTO actual = configurationService.get(conf.getSchema());
        actual.setSchemaInfo(null);
        assertEquals(actual, conf);
    }

    @Test
    public void createRequired() {
        PlainSchemaTO testKey = new PlainSchemaTO();
        testKey.setKey("testKey" + getUUIDString());
        testKey.setType(AttrSchemaType.String);
        testKey.setMandatoryCondition("true");
        createSchema(SchemaType.PLAIN, testKey);

        AttrTO conf = new AttrTO.Builder().schema(testKey.getKey()).build();
        try {
            configurationService.set(conf);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        conf.getValues().add("testValue");
        configurationService.set(conf);

        AttrTO actual = configurationService.get(conf.getSchema());
        actual.setSchemaInfo(null);
        assertEquals(actual, conf);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {
        try {
            configurationService.delete("nonExistent");
            fail("The delete operation should throw an exception because of nonExistent schema");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        AttrTO tokenLength = configurationService.get("token.length");

        configurationService.delete("token.length");

        AttrTO actual = configurationService.get(tokenLength.getSchema());
        assertNotEquals(actual, tokenLength);

        configurationService.set(tokenLength);

        actual = configurationService.get(tokenLength.getSchema());
        assertEquals(actual, tokenLength);
    }

    @Test
    public void list() {
        List<AttrTO> wholeConf = configurationService.list();
        assertNotNull(wholeConf);
        for (AttrTO conf : wholeConf) {
            assertNotNull(conf);
        }
    }

    @Test
    public void read() {
        AttrTO conf = configurationService.get("token.expireTime");
        assertNotNull(conf);
    }

    @Test
    public void update() {
        AttrTO expireTime = configurationService.get("token.expireTime");
        int value = Integer.parseInt(expireTime.getValues().get(0));
        value++;
        expireTime.getValues().set(0, value + "");

        configurationService.set(expireTime);

        AttrTO newConfigurationTO = configurationService.get(expireTime.getSchema());
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
        String configExport = IOUtils.toString((InputStream) entity, StandardCharsets.UTF_8.name());
        assertFalse(configExport.isEmpty());
        assertTrue(configExport.length() > 1000);
    }

    @Test
    public void issueSYNCOPE418() {
        PlainSchemaTO failing = new PlainSchemaTO();
        failing.setKey("http://schemas.examples.org/security/authorization/organizationUnit");
        failing.setType(AttrSchemaType.String);

        try {
            createSchema(SchemaType.PLAIN, failing);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPlainSchema, e.getType());

            assertNotNull(e.getElements());
            assertEquals(1, e.getElements().size());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidKey.name()));
        }
    }
}
