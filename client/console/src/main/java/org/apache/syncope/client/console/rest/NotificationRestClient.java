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
package org.apache.syncope.client.console.rest;

import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.slf4j.LoggerFactory;

public class NotificationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 6328933265096511690L;

    protected static final org.slf4j.Logger LOG = LoggerFactory.getLogger(NotificationRestClient.class);

    public List<NotificationTO> getAllNotifications() {
        return getService(NotificationService.class).list();
    }

    public NotificationTO read(final Long key) {
        return getService(NotificationService.class).read(key);
    }

    public void create(final NotificationTO notificationTO) {
        getService(NotificationService.class).create(notificationTO);
    }

    public void update(final NotificationTO notificationTO) {
        getService(NotificationService.class).update(notificationTO);
    }

    public void delete(final Long key) {
        getService(NotificationService.class).delete(key);
    }

    public List<MailTemplateTO> getAllAvailableTemplates() {
        return getService(MailTemplateService.class).list();
    }

    public void createTemplate(final MailTemplateTO mailTemplateTO) {
        getService(MailTemplateService.class).create(mailTemplateTO);
    }

    public void deleteTemplate(final String key) {
        getService(MailTemplateService.class).delete(key);
    }

    public MailTemplateTO readTemplate(final String key) {
        return getService(MailTemplateService.class).read(key);
    }

    public String readTemplateFormat(final String key, final MailTemplateFormat format) {
        try {
            return IOUtils.toString(InputStream.class.cast(
                    getService(MailTemplateService.class).getFormat(key, format).getEntity()));
        } catch (Exception e) {
            LOG.info("Error retrieving mail tenplate content");
            return StringUtils.EMPTY;
        }
    }

    public void updateTemplateFormat(final String key, final String str, final MailTemplateFormat format) {
        getService(MailTemplateService.class).setFormat(key, format, IOUtils.toInputStream(str));
    }
}
