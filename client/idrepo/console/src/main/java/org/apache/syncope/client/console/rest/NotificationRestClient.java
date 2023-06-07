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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.NotificationService;

public class NotificationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 6328933265096511690L;

    public List<NotificationTO> list() {
        return getService(NotificationService.class).list();
    }

    public JobTO getJob() {
        return getService(NotificationService.class).getJob();
    }

    public void actionJob(final JobAction jobAction) {
        getService(NotificationService.class).actionJob(jobAction);
    }

    public NotificationTO read(final String key) {
        return getService(NotificationService.class).read(key);
    }

    public void create(final NotificationTO notificationTO) {
        getService(NotificationService.class).create(notificationTO);
    }

    public void update(final NotificationTO notificationTO) {
        getService(NotificationService.class).update(notificationTO);
    }

    public void delete(final String key) {
        getService(NotificationService.class).delete(key);
    }

    public List<MailTemplateTO> listTemplates() {
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
                    getService(MailTemplateService.class).getFormat(key, format).getEntity()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Error retrieving mail template {} as {}", key, format, e);
            return StringUtils.EMPTY;
        }
    }

    public void updateTemplateFormat(final String key, final String content, final MailTemplateFormat format) {
        getService(MailTemplateService.class).setFormat(
                key, format, IOUtils.toInputStream(content, StandardCharsets.UTF_8));
    }
}
