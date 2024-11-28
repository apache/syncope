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
package org.apache.syncope.core.provisioning.java.data;

import java.text.ParseException;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDataBinderImpl implements NotificationDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(NotificationDataBinder.class);

    protected final MailTemplateDAO mailTemplateDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ImplementationDAO implementationDAO;

    protected final EntityFactory entityFactory;

    protected final IntAttrNameParser intAttrNameParser;

    public NotificationDataBinderImpl(
            final MailTemplateDAO mailTemplateDAO,
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final EntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser) {

        this.mailTemplateDAO = mailTemplateDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.implementationDAO = implementationDAO;
        this.entityFactory = entityFactory;
        this.intAttrNameParser = intAttrNameParser;
    }

    @Override
    public NotificationTO getNotificationTO(final Notification notification) {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setKey(notification.getKey());
        notificationTO.setTemplate(notification.getTemplate().getKey());
        notificationTO.getEvents().addAll(notification.getEvents());
        notificationTO.setRecipientsFIQL(notification.getRecipientsFIQL());
        notificationTO.getStaticRecipients().addAll(notification.getStaticRecipients());
        notificationTO.setRecipientAttrName(notification.getRecipientAttrName());
        notificationTO.setSelfAsRecipient(notification.isSelfAsRecipient());
        notificationTO.setSender(notification.getSender());
        notificationTO.setSubject(notification.getSubject());
        notificationTO.setTraceLevel(notification.getTraceLevel());
        notificationTO.setActive(notification.isActive());

        notification.getAbouts().forEach(about -> notificationTO.getAbouts().
                put(about.getAnyType().getKey(), about.get()));

        if (notification.getRecipientsProvider() != null) {
            notificationTO.setRecipientsProvider(notification.getRecipientsProvider().getKey());
        }

        return notificationTO;
    }

    @Override
    public Notification create(final NotificationTO notificationTO) {
        Notification result = entityFactory.newEntity(Notification.class);
        update(result, notificationTO);
        return result;
    }

    @Override
    public void update(final Notification notification, final NotificationTO notificationTO) {
        notification.setRecipientsFIQL(notificationTO.getRecipientsFIQL());

        notification.getStaticRecipients().clear();
        notification.getStaticRecipients().addAll(notificationTO.getStaticRecipients());

        notification.setRecipientAttrName(notificationTO.getRecipientAttrName());
        notification.setSelfAsRecipient(notificationTO.isSelfAsRecipient());
        notification.setSender(notificationTO.getSender());
        notification.setSubject(notificationTO.getSubject());
        notification.setTraceLevel(notificationTO.getTraceLevel());
        notification.setActive(notificationTO.isActive());

        notification.getEvents().clear();
        notification.getEvents().addAll(notificationTO.getEvents());

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        MailTemplate template = mailTemplateDAO.findById(notificationTO.getTemplate()).orElse(null);
        if (template == null) {
            sce.getElements().add("template");
        }
        notification.setTemplate(template);

        if (notification.getEvents().isEmpty()) {
            sce.getElements().add("events");
        }

        if (!notification.getStaticRecipients().isEmpty()) {
            notification.getStaticRecipients().forEach(mail -> {
                Matcher matcher = Entity.EMAIL_PATTERN.matcher(mail);
                if (!matcher.matches()) {
                    LOG.error("Invalid mail address: {}", mail);
                    sce.getElements().add("staticRecipients: " + mail);
                }
            });
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        // 1. add or update all (valid) abouts from TO
        notificationTO.getAbouts().entrySet().stream().
                filter(entry -> StringUtils.isNotBlank(entry.getValue())).
                forEach(entry -> anyTypeDAO.findById(entry.getKey()).ifPresentOrElse(
                anyType -> {
                    AnyAbout about = notification.getAbout(anyType).orElse(null);
                    if (about == null) {
                        about = entityFactory.newEntity(AnyAbout.class);
                        about.setAnyType(anyType);
                        about.setNotification(notification);

                        notification.add(about);
                    }
                    about.set(entry.getValue());
                },
                () -> LOG.debug("Invalid AnyType {} specified, ignoring...", entry.getKey())));

        // 2. remove all abouts not contained in the TO
        notification.getAbouts().
                removeIf(anyAbout -> !notificationTO.getAbouts().containsKey(anyAbout.getAnyType().getKey()));

        // 3. verify recipientAttrName
        try {
            intAttrNameParser.parse(notification.getRecipientAttrName(), AnyTypeKind.USER);
        } catch (ParseException e) {
            SyncopeClientException invalidRequest = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            invalidRequest.getElements().add(e.getMessage());
            throw invalidRequest;
        }

        if (notificationTO.getRecipientsProvider() == null) {
            notification.setRecipientsProvider(null);
        } else {
            implementationDAO.findById(notificationTO.getRecipientsProvider()).ifPresentOrElse(
                    notification::setRecipientsProvider,
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                        Implementation.class.getSimpleName(), notificationTO.getRecipientsProvider()));
        }
    }
}
