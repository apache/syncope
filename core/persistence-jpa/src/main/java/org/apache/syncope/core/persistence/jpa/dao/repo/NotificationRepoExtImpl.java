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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.jpa.entity.JPANotification;
import org.springframework.data.domain.Pageable;

public class NotificationRepoExtImpl implements NotificationRepoExt {

    protected final TaskDAO taskDAO;

    protected final EntityManager entityManager;

    public NotificationRepoExtImpl(final TaskDAO taskDAO, final EntityManager entityManager) {
        this.taskDAO = taskDAO;
        this.entityManager = entityManager;
    }

    @Override
    public Notification save(final Notification notification) {
        ((JPANotification) notification).list2json();
        return entityManager.merge(notification);
    }

    @Override
    public void deleteById(final String key) {
        Notification notification = entityManager.find(JPANotification.class, key);
        if (notification == null) {
            return;
        }

        taskDAO.findAll(
                TaskType.NOTIFICATION, null, notification, null, null, Pageable.unpaged()).
                stream().map(Task::getKey).forEach(taskDAO::deleteById);

        entityManager.remove(notification);
    }
}
