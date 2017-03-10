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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class MailTemplateITCase extends AbstractITCase {

    @Test
    public void read() {
        MailTemplateTO mailTemplateTO = mailTemplateService.read("optin");
        assertNotNull(mailTemplateTO);
    }

    @Test
    public void list() {
        List<MailTemplateTO> mailTemplateTOs = mailTemplateService.list();
        assertNotNull(mailTemplateTOs);
        assertFalse(mailTemplateTOs.isEmpty());
        for (MailTemplateTO instance : mailTemplateTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void crud() throws IOException {
        final String key = getUUIDString();

        // 1. create (empty) mail template
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        mailTemplateTO.setKey(key);

        Response response = mailTemplateService.create(mailTemplateTO);
        assertEquals(201, response.getStatus());

        // 2. attempt to read HTML and TEXT -> fail
        try {
            mailTemplateService.getFormat(key, MailTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            mailTemplateService.getFormat(key, MailTemplateFormat.TEXT);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. set TEXT
        String textTemplate = "Hi there, I am ${username}.";
        mailTemplateService.setFormat(
                key, MailTemplateFormat.TEXT, IOUtils.toInputStream(textTemplate, StandardCharsets.UTF_8));

        response = mailTemplateService.getFormat(key, MailTemplateFormat.TEXT);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_PLAIN));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                textTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 3. set HTML
        String htmlTemplate = "<html><body>Hi there, I am ${username}.</body></html>";
        mailTemplateService.setFormat(
                key, MailTemplateFormat.HTML, IOUtils.toInputStream(htmlTemplate, StandardCharsets.UTF_8));

        response = mailTemplateService.getFormat(key, MailTemplateFormat.HTML);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_HTML));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                htmlTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 4. remove HTML
        mailTemplateService.removeFormat(key, MailTemplateFormat.HTML);

        try {
            mailTemplateService.getFormat(key, MailTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        response = mailTemplateService.getFormat(key, MailTemplateFormat.TEXT);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_PLAIN));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                textTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 5. remove mail template
        mailTemplateService.delete(key);

        try {
            mailTemplateService.read(key);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            mailTemplateService.getFormat(key, MailTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            mailTemplateService.getFormat(key, MailTemplateFormat.TEXT);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE866() {
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        mailTemplateTO.setKey("optin");
        try {
            mailTemplateService.create(mailTemplateTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
