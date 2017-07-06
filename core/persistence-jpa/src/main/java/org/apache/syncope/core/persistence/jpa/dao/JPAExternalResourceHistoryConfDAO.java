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
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceHistoryConfDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResourceHistoryConf;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResourceHistoryConf;
import org.springframework.stereotype.Repository;

@Repository
public class JPAExternalResourceHistoryConfDAO
        extends AbstractDAO<ExternalResourceHistoryConf> implements ExternalResourceHistoryConfDAO {

    @Override
    public ExternalResourceHistoryConf find(final String key) {
        return entityManager().find(JPAExternalResourceHistoryConf.class, key);
    }

    @Override
    public List<ExternalResourceHistoryConf> findByEntity(final ExternalResource entity) {
        TypedQuery<ExternalResourceHistoryConf> query = entityManager().createQuery(
                "SELECT e FROM " + JPAExternalResourceHistoryConf.class.getSimpleName()
                + " e WHERE e.entity=:entity ORDER BY e.creation DESC", ExternalResourceHistoryConf.class);
        query.setParameter("entity", entity);
        return query.getResultList();
    }

    @Override
    public ExternalResourceHistoryConf save(final ExternalResourceHistoryConf conf) {
        return entityManager().merge(conf);
    }

    @Override
    public void delete(final String key) {
        ExternalResourceHistoryConf conf = find(key);
        if (conf == null) {
            return;
        }

        entityManager().remove(conf);
    }

    @Override
    public void deleteByEntity(final ExternalResource entity) {
        Query query = entityManager().createQuery(
                "DELETE FROM " + JPAExternalResourceHistoryConf.class.getSimpleName() + " e WHERE e.entity=:entity");
        query.setParameter("entity", entity);
        query.executeUpdate();
    }

}
