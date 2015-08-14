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

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.jpa.entity.JPANotification;
import org.springframework.stereotype.Repository;

@Repository
public class JPANotificationDAO extends AbstractDAO<Notification, Long> implements NotificationDAO {

    @Override
    public Notification find(final Long key) {
        return entityManager().find(JPANotification.class, key);
    }

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
    public void delete(final Long key) {
        entityManager().remove(find(key));
    }
}
