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
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.persistence.jpa.entity.JPALogger;
import org.springframework.stereotype.Repository;

@Repository
public class JPALoggerDAO extends AbstractDAO<Logger> implements LoggerDAO {

    @Override
    public Logger find(final String key) {
        return entityManager().find(JPALogger.class, key);
    }

    @Override
    public List<Logger> findAll(final LoggerType type) {
        TypedQuery<Logger> query = entityManager().createQuery(
                "SELECT e FROM " + JPALogger.class.getSimpleName() + " e WHERE e.type=:type", Logger.class);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public Logger save(final Logger logger) {
        // Audit loggers must be either OFF or DEBUG, no more options
        if (LoggerType.AUDIT == logger.getType() && LoggerLevel.OFF != logger.getLevel()) {
            logger.setLevel(LoggerLevel.DEBUG);
        }
        return entityManager().merge(logger);
    }

    @Override
    public void delete(final Logger logger) {
        entityManager().remove(logger);
    }

    @Override
    public void delete(final String key) {
        Logger logger = find(key);
        if (logger == null) {
            return;
        }

        delete(logger);
    }
}
