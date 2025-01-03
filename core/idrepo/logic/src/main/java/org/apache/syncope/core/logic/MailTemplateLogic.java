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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class MailTemplateLogic extends AbstractTransactionalLogic<MailTemplateTO> {

    protected final MailTemplateDAO mailTemplateDAO;

    protected final NotificationDAO notificationDAO;

    protected final EntityFactory entityFactory;

    public MailTemplateLogic(
            final MailTemplateDAO mailTemplateDAO,
            final NotificationDAO notificationDAO,
            final EntityFactory entityFactory) {

        this.mailTemplateDAO = mailTemplateDAO;
        this.notificationDAO = notificationDAO;
        this.entityFactory = entityFactory;
    }

    protected MailTemplateTO getMailTemplateTO(final String key) {
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        mailTemplateTO.setKey(key);
        return mailTemplateTO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_READ + "')")
    @Transactional(readOnly = true)
    public MailTemplateTO read(final String key) {
        mailTemplateDAO.findById(key).
                orElseThrow(() -> new NotFoundException("MailTemplate " + key));

        return getMailTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_LIST + "')")
    @Transactional(readOnly = true)
    public List<MailTemplateTO> list() {
        return mailTemplateDAO.findAll().stream().
                map(template -> getMailTemplateTO(template.getKey())).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_CREATE + "')")
    public MailTemplateTO create(final String key) {
        if (mailTemplateDAO.existsById(key)) {
            throw new DuplicateException(key);
        }

        MailTemplate mailTemplate = entityFactory.newEntity(MailTemplate.class);
        mailTemplate.setKey(key);
        mailTemplateDAO.save(mailTemplate);

        return getMailTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_READ + "')")
    public String getFormat(final String key, final MailTemplateFormat format) {
        MailTemplate mailTemplate = mailTemplateDAO.findById(key).
                orElseThrow(() -> new NotFoundException("MailTemplate " + key));

        String template = format == MailTemplateFormat.HTML
                ? mailTemplate.getHTMLTemplate()
                : mailTemplate.getTextTemplate();
        if (StringUtils.isBlank(template)) {
            LOG.error("Could not find mail template '{}' in {} format", key, format);

            throw new NotFoundException(key + " in " + format);
        }

        return template;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_UPDATE + "')")
    public void setFormat(final String key, final MailTemplateFormat format, final String template) {
        MailTemplate mailTemplate = mailTemplateDAO.findById(key).
                orElseThrow(() -> new NotFoundException("MailTemplate " + key));

        if (format == MailTemplateFormat.HTML) {
            mailTemplate.setHTMLTemplate(template);
        } else {
            mailTemplate.setTextTemplate(template);
        }

        mailTemplateDAO.save(mailTemplate);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MAIL_TEMPLATE_DELETE + "')")
    public MailTemplateTO delete(final String key) {
        MailTemplate mailTemplate = mailTemplateDAO.findById(key).
                orElseThrow(() -> new NotFoundException("MailTemplate " + key));

        List<Notification> notifications = notificationDAO.findByTemplate(mailTemplate);
        if (!notifications.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InUse);
            sce.getElements().addAll(notifications.stream().map(Notification::getKey).toList());
            throw sce;
        }

        MailTemplateTO deleted = getMailTemplateTO(key);
        mailTemplateDAO.deleteById(key);
        return deleted;
    }

    @Override
    protected MailTemplateTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof MailTemplateTO mailTemplateTO) {
                    key = mailTemplateTO.getKey();
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
