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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MailTemplateTest extends AbstractTest {

    @Autowired
    private MailTemplateDAO mailTemplateDAO;

    @Test
    public void find() {
        MailTemplate optin = mailTemplateDAO.findById("optin").orElseThrow();
        assertNotNull(optin.getTextTemplate());
        assertNotNull(optin.getHTMLTemplate());
    }

    @Test
    public void findAll() {
        List<? extends MailTemplate> templates = mailTemplateDAO.findAll();
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
    }

    @Test
    public void save() {
        MailTemplate template = entityFactory.newEntity(MailTemplate.class);
        template.setKey("new");
        template.setTextTemplate("Text template");

        MailTemplate actual = mailTemplateDAO.save(template);
        assertNotNull(actual);
        assertNotNull(actual.getKey());
        assertNotNull(actual.getTextTemplate());
        assertNull(actual.getHTMLTemplate());

        actual.setHTMLTemplate("<html><body><p>HTML template</p></body></html>");
        actual = mailTemplateDAO.save(actual);
        assertNotNull(actual.getTextTemplate());
        assertNotNull(actual.getHTMLTemplate());
    }

    @Test
    public void delete() {
        mailTemplateDAO.deleteById("optin");
        assertTrue(mailTemplateDAO.findById("optin").isEmpty());
    }
}
