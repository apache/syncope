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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XMLContentExporterTest extends AbstractTest {

    @Autowired
    private ContentExporter exporter;

    private static void checkRealms(final String exported) {
        List<String> realms = exported.lines().filter(row -> row.trim().startsWith("<Realm ")).toList();
        assertEquals(4, realms.size());
        assertTrue(realms.get(0).contains("name=\"/\""));
        assertTrue(realms.get(1).contains("name=\"even\""));
        assertTrue(realms.get(2).contains("name=\"two\""));
        assertTrue(realms.get(3).contains("name=\"odd\""));
    }

    /**
     * Also checks for SYNCOPE-1307.
     *
     * @throws Exception exception thrown when dealing with IO.
     */
    @Test
    public void issueSYNCOPE1128() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        exporter.export(SyncopeConstants.MASTER_DOMAIN, 100, baos);

        String exported = baos.toString(StandardCharsets.UTF_8);
        assertTrue(StringUtils.isNotBlank(exported));

        checkRealms(exported);
    }

    @Test
    public void exportElements() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        exporter.export(SyncopeConstants.MASTER_DOMAIN, 100, baos, "AccountPolicy", "Realm");

        String exported = baos.toString(StandardCharsets.UTF_8);
        assertTrue(StringUtils.isNotBlank(exported));

        List<String> accountPolicies = exported.lines().
                filter(row -> row.trim().startsWith("<AccountPolicy ")).toList();
        assertEquals(2, accountPolicies.size());

        checkRealms(exported);
    }
}
