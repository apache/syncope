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

import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataBinderImpl implements NotificationDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDataBinder.class);

    private static final String[] IGNORE_PROPERTIES = { "key", "abouts" };

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public NotificationTO getNotificationTO(final Notification notification) {
        NotificationTO result = new NotificationTO();

        BeanUtils.copyProperties(notification, result, IGNORE_PROPERTIES);

        result.setKey(notification.getKey());
        for (AnyAbout about : notification.getAbouts()) {
            result.getAbouts().put(about.getAnyType().getKey(), about.get());
        }

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

        // 1. add or update all (valid) abouts from TO
        for (Map.Entry<String, String> entry : notificationTO.getAbouts().entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                AnyType anyType = anyTypeDAO.find(entry.getKey());
                if (anyType == null) {
                    LOG.debug("Invalid AnyType {} specified, ignoring...", entry.getKey());
                } else {
                    AnyAbout about = notification.getAbout(anyType);
                    if (about == null) {
                        about = entityFactory.newEntity(AnyAbout.class);
                        about.setAnyType(anyType);
                        about.setNotification(notification);

                        notification.add(about);
                    }
                    about.set(entry.getValue());
                }
            }
        }

        // 2. remove all abouts not contained in the TO
        CollectionUtils.filter(notification.getAbouts(), new Predicate<AnyAbout>() {

            @Override
            public boolean evaluate(final AnyAbout anyAbout) {
                return notificationTO.getAbouts().containsKey(anyAbout.getAnyType().getKey());
            }
        });
    }
}
