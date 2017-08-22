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

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class MailTemplateLogic extends AbstractTransactionalLogic<MailTemplateTO> {

    @Autowired
    private MailTemplateDAO mailTemplateDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private EntityFactory entityFactory;

    private MailTemplateTO getMailTemplateTO(final String key) {
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        mailTemplateTO.setKey(key);
        return mailTemplateTO;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_READ + "')")
    public MailTemplateTO read(final String key) {
        MailTemplate mailTemplate = mailTemplateDAO.find(key);
        if (mailTemplate == null) {
            LOG.error("Could not find mail template '" + key + "'");

            throw new NotFoundException(key);
        }

        return getMailTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_LIST + "')")
    public List<MailTemplateTO> list() {
        return mailTemplateDAO.findAll().stream().
                map(template -> getMailTemplateTO(template.getKey())).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_CREATE + "')")
    public MailTemplateTO create(final String key) {
        if (mailTemplateDAO.find(key) != null) {
            throw new DuplicateException(key);
        }
        MailTemplate mailTemplate = entityFactory.newEntity(MailTemplate.class);
        mailTemplate.setKey(key);
        mailTemplateDAO.save(mailTemplate);

        return getMailTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_READ + "')")
    public String getFormat(final String key, final MailTemplateFormat format) {
        MailTemplate mailTemplate = mailTemplateDAO.find(key);
        if (mailTemplate == null) {
            LOG.error("Could not find mail template '" + key + "'");

            throw new NotFoundException(key);
        }

        String template = format == MailTemplateFormat.HTML
                ? mailTemplate.getHTMLTemplate()
                : mailTemplate.getTextTemplate();
        if (StringUtils.isBlank(template)) {
            LOG.error("Could not find mail template '" + key + "' in " + format + " format");

            throw new NotFoundException(key + " in " + format);
        }

        return template;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_UPDATE + "')")
    public void setFormat(final String key, final MailTemplateFormat format, final String template) {
        MailTemplate mailTemplate = mailTemplateDAO.find(key);
        if (mailTemplate == null) {
            LOG.error("Could not find mail template '" + key + "'");

            throw new NotFoundException(key);
        }

        if (format == MailTemplateFormat.HTML) {
            mailTemplate.setHTMLTemplate(template);
        } else {
            mailTemplate.setTextTemplate(template);
        }

        mailTemplateDAO.save(mailTemplate);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MAIL_TEMPLATE_DELETE + "')")
    public MailTemplateTO delete(final String key) {
        MailTemplate mailTemplate = mailTemplateDAO.find(key);
        if (mailTemplate == null) {
            LOG.error("Could not find mail template '" + key + "'");

            throw new NotFoundException(key);
        }

        List<Notification> notifications = notificationDAO.findByTemplate(mailTemplate);
        if (!notifications.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InUseByNotifications);
            sce.getElements().addAll(notifications.stream().map(Entity::getKey).collect(Collectors.toList()));
            throw sce;
        }

        MailTemplateTO deleted = getMailTemplateTO(key);
        mailTemplateDAO.delete(key);
        return deleted;
    }

    @Override
    protected MailTemplateTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = ((String) args[i]);
                } else if (args[i] instanceof MailTemplateTO) {
                    key = ((MailTemplateTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return getMailTemplateTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
