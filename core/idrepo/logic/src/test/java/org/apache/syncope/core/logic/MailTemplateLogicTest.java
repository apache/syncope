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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MailTemplateLogicTest {

    private MailTemplateDAO mailTemplateDAO;

    private NotificationDAO notificationDAO;

    private EntityFactory entityFactory;

    private MailTemplateLogic logic;

    @BeforeEach
    void setUp() {
        mailTemplateDAO = mock(MailTemplateDAO.class);
        notificationDAO = mock(NotificationDAO.class);
        entityFactory = mock(EntityFactory.class);
        logic = new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(mailTemplateDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void read() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");

        assertEquals("key", logic.read("key").getKey());
    }

    @Test
    void list() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        when(mailTemplate.getKey()).thenReturn("key");
        doReturn(List.of(mailTemplate)).when(mailTemplateDAO).findAll();

        assertEquals(List.of("key"), logic.list().stream().map(MailTemplateTO::getKey).toList());
    }

    @Test
    void createDuplicate() {
        when(mailTemplateDAO.existsById("key")).thenReturn(true);

        assertThrows(DuplicateException.class, () -> logic.create("key"));
    }

    @Test
    void create() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        when(entityFactory.newEntity(MailTemplate.class)).thenReturn(mailTemplate);

        assertEquals("key", logic.create("key").getKey());
        verify(mailTemplate).setKey("key");
        verify(mailTemplateDAO).save(mailTemplate);
    }

    @Test
    void getFormatMissing() {
        doReturn(Optional.empty()).when(mailTemplateDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.getFormat("missing", MailTemplateFormat.TEXT));
    }

    @Test
    void getFormatWithoutContent() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.getFormat("key", MailTemplateFormat.HTML));
    }

    @Test
    void getFormat() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");
        when(mailTemplate.getTextTemplate()).thenReturn("text");
        when(mailTemplate.getHTMLTemplate()).thenReturn("html");

        assertEquals("text", logic.getFormat("key", MailTemplateFormat.TEXT));
        assertEquals("html", logic.getFormat("key", MailTemplateFormat.HTML));
    }

    @Test
    void setFormat() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");

        logic.setFormat("key", MailTemplateFormat.TEXT, "text");
        logic.setFormat("key", MailTemplateFormat.HTML, "html");

        verify(mailTemplate).setTextTemplate("text");
        verify(mailTemplate).setHTMLTemplate("html");
        verify(mailTemplateDAO, org.mockito.Mockito.times(2)).save(mailTemplate);
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(mailTemplateDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void deleteInUse() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        Notification notification = mock(Notification.class);
        when(notification.getKey()).thenReturn("notification");
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");
        when(notificationDAO.findByTemplate(mailTemplate)).thenReturn(List.of(notification));

        assertThrows(SyncopeClientException.class, () -> logic.delete("key"));
    }

    @Test
    void delete() {
        MailTemplate mailTemplate = mock(MailTemplate.class);
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById("key");
        when(notificationDAO.findByTemplate(mailTemplate)).thenReturn(List.of());

        assertEquals("key", logic.delete("key").getKey());
        verify(mailTemplateDAO).deleteById("key");
    }

    @Test
    void resolveBeanReference() throws Exception {
        MailTemplateTO to = new MailTemplateTO();
        to.setKey("key");

        assertSame(
                to.getKey(),
                logic.resolveBeanReference(
                        MailTemplateLogic.class.getMethod("create", String.class), "key").getKey());
    }
}
