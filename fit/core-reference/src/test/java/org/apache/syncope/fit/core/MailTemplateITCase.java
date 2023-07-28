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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class MailTemplateITCase extends AbstractITCase {

    @Test
    public void read() {
        MailTemplateTO mailTemplateTO = MAIL_TEMPLATE_SERVICE.read("optin");
        assertNotNull(mailTemplateTO);
    }

    @Test
    public void list() {
        List<MailTemplateTO> mailTemplateTOs = MAIL_TEMPLATE_SERVICE.list();
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

        Response response = MAIL_TEMPLATE_SERVICE.create(mailTemplateTO);
        assertEquals(201, response.getStatus());

        // 2. attempt to read HTML and TEXT -> fail
        try {
            MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.HTML);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.TEXT);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. set TEXT
        String textTemplate = "Hi there, I am ${username}.";
        MAIL_TEMPLATE_SERVICE.setFormat(
                key, MailTemplateFormat.TEXT, IOUtils.toInputStream(textTemplate, StandardCharsets.UTF_8));

        response = MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.TEXT);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_PLAIN));
        assertEquals(textTemplate, response.readEntity(String.class));

        // 3. set HTML
        String htmlTemplate = "<html><body>Hi there, I am ${username}.</body></html>";
        MAIL_TEMPLATE_SERVICE.setFormat(
                key, MailTemplateFormat.HTML, IOUtils.toInputStream(htmlTemplate, StandardCharsets.UTF_8));

        response = MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.HTML);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_HTML));
        assertEquals(htmlTemplate, response.readEntity(String.class));

        // 4. remove HTML
        MAIL_TEMPLATE_SERVICE.removeFormat(key, MailTemplateFormat.HTML);

        try {
            MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.HTML);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        response = MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.TEXT);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_PLAIN));
        assertEquals(textTemplate, response.readEntity(String.class));

        // 5. remove mail template
        MAIL_TEMPLATE_SERVICE.delete(key);

        try {
            MAIL_TEMPLATE_SERVICE.read(key);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.HTML);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            MAIL_TEMPLATE_SERVICE.getFormat(key, MailTemplateFormat.TEXT);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE866() {
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        mailTemplateTO.setKey("optin");
        try {
            MAIL_TEMPLATE_SERVICE.create(mailTemplateTO);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
