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
import javax.persistence.TypedQuery;
import org.apache.syncope.common.types.SyncopeLoggerLevel;
import org.apache.syncope.common.types.SyncopeLoggerType;
import org.apache.syncope.core.persistence.beans.SyncopeLogger;
import org.apache.syncope.core.persistence.dao.LoggerDAO;
import org.springframework.stereotype.Repository;

@Repository
public class LoggerDAOImpl extends AbstractDAOImpl implements LoggerDAO {

    @Override
    public SyncopeLogger find(final String name) {
        return entityManager.find(SyncopeLogger.class, name);
    }

    @Override
    public List<SyncopeLogger> findAll(final SyncopeLoggerType type) {
        TypedQuery<SyncopeLogger> query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeLogger.class.getSimpleName() + " e WHERE e.type=:type", SyncopeLogger.class);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public SyncopeLogger save(final SyncopeLogger logger) {
        // Audit loggers must be either OFF or DEBUG, no more options
        if (SyncopeLoggerType.AUDIT == logger.getType() && SyncopeLoggerLevel.OFF != logger.getLevel()) {
            logger.setLevel(SyncopeLoggerLevel.DEBUG);
        }
        return entityManager.merge(logger);
    }

    @Override
    public void delete(final SyncopeLogger logger) {
        entityManager.remove(logger);
    }

    @Override
    public void delete(final String name) {
        SyncopeLogger logger = find(name);
        if (logger == null) {
            return;
        }

        delete(logger);
    }
}
