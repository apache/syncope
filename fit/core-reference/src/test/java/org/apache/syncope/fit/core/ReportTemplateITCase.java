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
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class ReportTemplateITCase extends AbstractITCase {

    @Test
    public void read() {
        ReportTemplateTO reportTemplateTO = reportTemplateService.read("sample");
        assertNotNull(reportTemplateTO);
    }

    @Test
    public void list() {
        List<ReportTemplateTO> reportTemplateTOs = reportTemplateService.list();
        assertNotNull(reportTemplateTOs);
        assertFalse(reportTemplateTOs.isEmpty());
        for (ReportTemplateTO instance : reportTemplateTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void crud() throws IOException {
        final String key = getUUIDString();

        // 1. create (empty) report template
        ReportTemplateTO reportTemplateTO = new ReportTemplateTO();
        reportTemplateTO.setKey(key);

        Response response = reportTemplateService.create(reportTemplateTO);
        assertEquals(201, response.getStatus());

        // 2. attempt to read HTML and CSV -> fail
        try {
            reportTemplateService.getFormat(key, ReportTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            reportTemplateService.getFormat(key, ReportTemplateFormat.CSV);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. set CSV
        String csvTemplate =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'></xsl:stylesheet>";
        reportTemplateService.setFormat(
                key, ReportTemplateFormat.CSV, IOUtils.toInputStream(csvTemplate, StandardCharsets.UTF_8));

        response = reportTemplateService.getFormat(key, ReportTemplateFormat.CSV);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_XML));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                csvTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 3. set HTML
        String htmlTemplate =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'></xsl:stylesheet>";
        reportTemplateService.setFormat(
                key, ReportTemplateFormat.HTML, IOUtils.toInputStream(htmlTemplate, StandardCharsets.UTF_8));

        response = reportTemplateService.getFormat(key, ReportTemplateFormat.HTML);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_XML));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                htmlTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 4. remove HTML
        reportTemplateService.removeFormat(key, ReportTemplateFormat.HTML);

        try {
            reportTemplateService.getFormat(key, ReportTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        response = reportTemplateService.getFormat(key, ReportTemplateFormat.CSV);
        assertEquals(200, response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_XML));
        assertTrue(response.getEntity() instanceof InputStream);
        assertEquals(
                csvTemplate,
                IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));

        // 5. remove report template
        reportTemplateService.delete(key);

        try {
            reportTemplateService.read(key);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            reportTemplateService.getFormat(key, ReportTemplateFormat.HTML);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            reportTemplateService.getFormat(key, ReportTemplateFormat.CSV);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE866() {
        ReportTemplateTO reportTemplateTO = new ReportTemplateTO();
        reportTemplateTO.setKey("empty");
        try {
            reportTemplateService.create(reportTemplateTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
