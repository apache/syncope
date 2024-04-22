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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.List;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyAbout;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jMailTemplate;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jNotification;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class NotificationRepoExtImpl extends AbstractDAO implements NotificationRepoExt {

    protected final TaskDAO taskDAO;

    protected final NodeValidator nodeValidator;

    public NotificationRepoExtImpl(
            final TaskDAO taskDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.taskDAO = taskDAO;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Notification> findByTemplate(final MailTemplate template) {
        return findByRelationship(
                Neo4jNotification.NODE,
                Neo4jMailTemplate.NODE,
                template.getKey(),
                Neo4jNotification.class,
                null);
    }

    @Override
    public Notification save(final Notification notification) {
        ((Neo4jNotification) notification).list2json();
        Notification saved = neo4jTemplate.save(nodeValidator.validate(notification));
        ((Neo4jNotification) saved).postSave();
        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jNotification.class).ifPresent(notification -> {
            taskDAO.findAll(
                    TaskType.NOTIFICATION, null, notification, null, null, Pageable.unpaged()).
                    stream().map(Task::getKey).forEach(taskDAO::deleteById);

            cascadeDelete(
                    Neo4jAnyAbout.NODE,
                    Neo4jNotification.NODE,
                    key);

            neo4jTemplate.deleteById(notification.getKey(), Neo4jNotification.class);
        });
    }
}
