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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.validation.InvalidEntityException;

@Repository
public class NotificationDAOImpl extends AbstractDAOImpl implements NotificationDAO {

    @Override
    public Notification find(final Long id) {
        return entityManager.find(Notification.class, id);
    }

    @Override
    public List<Notification> findAll() {
        Query query = entityManager.createQuery("SELECT e " + "FROM " + Notification.class.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public Notification save(final Notification notification) throws InvalidEntityException {

        return entityManager.merge(notification);
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));
    }
}
