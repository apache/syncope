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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ConfigurationITCase extends AbstractITCase {

    @Test
    public void create() {
        PlainSchemaTO testKey = new PlainSchemaTO();
        testKey.setKey("testKey" + getUUIDString());
        testKey.setType(AttrSchemaType.String);
        createSchema(SchemaType.PLAIN, testKey);

        AttrTO conf = new AttrTO();
        conf.setSchema(testKey.getKey());
        conf.getValues().add("testValue");

        configurationService.set(conf.getSchema(), conf);

        AttrTO actual = configurationService.read(conf.getSchema());
        assertEquals(actual, conf);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {
        try {
            configurationService.delete("nonExistent");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        AttrTO tokenLength = configurationService.read("token.length");

        configurationService.delete("token.length");
        try {
            configurationService.read("token.length");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        configurationService.set(tokenLength.getSchema(), tokenLength);

        AttrTO actual = configurationService.read(tokenLength.getSchema());
        assertEquals(actual, tokenLength);
    }

    @Test
    public void list() {
        ConfTO wholeConf = configurationService.list();
        assertNotNull(wholeConf);
        for (AttrTO conf : wholeConf.getPlainAttrs()) {
            assertNotNull(conf);
        }
    }

    @Test
    public void read() {
        AttrTO conf = configurationService.read("token.expireTime");
        assertNotNull(conf);
    }

    @Test
    public void update() {
        AttrTO expireTime = configurationService.read("token.expireTime");
        int value = Integer.parseInt(expireTime.getValues().get(0));
        value++;
        expireTime.getValues().set(0, value + "");

        configurationService.set(expireTime.getSchema(), expireTime);

        AttrTO newConfigurationTO = configurationService.read(expireTime.getSchema());
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
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidName.name()));
        }
    }

    private static String[] substringsBetween(final String str, final String open, final String close) {
        if (str == null || StringUtils.isEmpty(open) || StringUtils.isEmpty(close)) {
            return null;
        }
        final int strLen = str.length();
        if (strLen == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final int closeLen = close.length();
        final int openLen = open.length();
        final List<String> list = new ArrayList<>();
        int pos = 0;
        while (pos < strLen - closeLen) {
            int start = StringUtils.indexOfIgnoreCase(str, open, pos);
            if (start < 0) {
                break;
            }
            start += openLen;
            final int end = StringUtils.indexOfIgnoreCase(str, close, start);
            if (end < 0) {
                break;
            }
            list.add(str.substring(start, end));
            pos = end + closeLen;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    @Test
    public void issueSYNCOPE629() throws IOException {
        PlainSchemaTO membershipKey = new PlainSchemaTO();
        membershipKey.setKey("membershipKey" + getUUIDString());
        membershipKey.setType(AttrSchemaType.String);
        createSchema(SchemaType.PLAIN, membershipKey);

        PlainSchemaTO groupKey = new PlainSchemaTO();
        groupKey.setKey("group"
                + "Key" + getUUIDString());
        groupKey.setType(AttrSchemaType.String);
        createSchema(SchemaType.PLAIN, groupKey);

        GroupTO groupTO = new GroupTO();
        groupTO.setRealm("/");
        groupTO.setName("aGroup" + getUUIDString());
        groupTO = createGroup(groupTO);

        try {
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

            String[] result = substringsBetween(configExport, "<GPLAINATTRTEMPLATE", "/>");
            assertNotNull(result);
            boolean rattrExists = false;
            for (String entry : result) {
                if (entry.contains(groupKey.getKey())) {
                    rattrExists = true;
                }
            }
            assertTrue(rattrExists);

            result = substringsBetween(configExport, "<MPLAINATTRTEMPLATE", "/>");
            assertNotNull(result);
            boolean mattrExists = false;
            for (String entry : result) {
                if (entry.contains(membershipKey.getKey())) {
                    mattrExists = true;
                }
            }
            assertTrue(mattrExists);
        } finally {
            deleteGroup(groupTO.getKey());
        }
    }
}
