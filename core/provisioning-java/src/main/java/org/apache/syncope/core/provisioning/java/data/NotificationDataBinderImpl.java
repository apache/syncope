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

import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataBinderImpl implements NotificationDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDataBinder.class);

    private static final String[] IGNORE_PROPERTIES = { "key", "template", "abouts" };

    @Autowired
    private MailTemplateDAO mailTemplateDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Override
    public NotificationTO getNotificationTO(final Notification notification) {
        NotificationTO result = new NotificationTO();
        result.setKey(notification.getKey());
        result.setTemplate(notification.getTemplate().getKey());

        BeanUtils.copyProperties(notification, result, IGNORE_PROPERTIES);

        notification.getAbouts().forEach(about -> {
            result.getAbouts().put(about.getAnyType().getKey(), about.get());
        });

        return result;
    }

    @Override
    public Notification create(final NotificationTO notificationTO) {
        Notification result = entityFactory.newEntity(Notification.class);
        update(result, notificationTO);
        return result;
    }

    @Override
    public void update(final Notification notification, final NotificationTO notificationTO) {
        BeanUtils.copyProperties(notificationTO, notification, IGNORE_PROPERTIES);

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        MailTemplate template = mailTemplateDAO.find(notificationTO.getTemplate());
        if (template == null) {
            sce.getElements().add("template");
        }
        notification.setTemplate(template);

        if (notification.getEvents().isEmpty()) {
            sce.getElements().add("events");
        }

        if (!notification.getStaticRecipients().isEmpty()) {
            notification.getStaticRecipients().forEach(mail -> {
                Matcher matcher = SyncopeConstants.EMAIL_PATTERN.matcher(mail);
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
                forEachOrdered((entry) -> {

                    AnyType anyType = anyTypeDAO.find(entry.getKey());
                    if (anyType == null) {
                        LOG.debug("Invalid AnyType {} specified, ignoring...", entry.getKey());
                    } else {
                        AnyAbout about = notification.getAbout(anyType).orElse(null);
                        if (about == null) {
                            about = entityFactory.newEntity(AnyAbout.class);
                            about.setAnyType(anyType);
                            about.setNotification(notification);

                            notification.add(about);
                        }
                        about.set(entry.getValue());
                    }
                });

        // 2. remove all abouts not contained in the TO
        notification.getAbouts().removeAll(notification.getAbouts().stream().
                filter(anyAbout -> !notificationTO.getAbouts().containsKey(anyAbout.getAnyType().getKey())).
                collect(Collectors.toList()));

        // 3. verify recipientAttrName
        intAttrNameParser.parse(notification.getRecipientAttrName(), AnyTypeKind.USER);
    }
}
