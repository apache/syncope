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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Collections;
import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.jpa.entity.JPANotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPANotificationDAO extends AbstractDAO<Notification> implements NotificationDAO {

    @Autowired
    private TaskDAO taskDAO;

    @Transactional(readOnly = true)
    @Override
    public Notification find(final String key) {
        return entityManager().find(JPANotification.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Notification> findByTemplate(final MailTemplate template) {
        TypedQuery<Notification> query = entityManager().createQuery(
                "SELECT e FROM " + JPANotification.class.getSimpleName() + " e "
                + "WHERE e.template=:template", Notification.class);
        query.setParameter("template", template);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Notification> findAll() {
        TypedQuery<Notification> query = entityManager().createQuery(
                "SELECT e FROM " + JPANotification.class.getSimpleName() + " e", Notification.class);
        return query.getResultList();
    }

    @Override
    public Notification save(final Notification notification) {
        return entityManager().merge(notification);
    }

    @Override
    public void delete(final String key) {
        Notification notification = find(key);
        if (notification == null) {
            return;
        }

        taskDAO.findAll(
                TaskType.NOTIFICATION, null, notification, null, null, -1, -1, Collections.<OrderByClause>emptyList()).
                stream().map(Entity::getKey).forEach(task -> delete(task));

        entityManager().remove(notification);
    }
}
