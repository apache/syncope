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
import java.util.concurrent.CopyOnWriteArrayList;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.provisioning.api.ConnectorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAConnInstanceDAO extends AbstractDAO<ConnInstance> implements ConnInstanceDAO {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnectorRegistry connRegistry;

    @Override
    public ConnInstance find(final String key) {
        return entityManager().find(JPAConnInstance.class, key);
    }

    @Override
    public List<ConnInstance> findAll() {
        TypedQuery<ConnInstance> query = entityManager().createQuery(
                "SELECT e FROM " + JPAConnInstance.class.getSimpleName() + " e", ConnInstance.class);
        return query.getResultList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        final ConnInstance merged = entityManager().merge(connector);

        for (ExternalResource resource : merged.getResources()) {
            try {
                connRegistry.registerConnector(resource);
            } catch (NotFoundException e) {
                LOG.error("While registering connector for resource", e);
            }
        }

        return merged;
    }

    @Override
    public void delete(final String key) {
        ConnInstance connInstance = find(key);
        if (connInstance == null) {
            return;
        }

        IterableUtils.forEach(new CopyOnWriteArrayList<>(connInstance.getResources()), new Closure<ExternalResource>() {

            @Override
            public void execute(final ExternalResource input) {
                resourceDAO.delete(input.getKey());
            }

        });

        entityManager().remove(connInstance);

        connRegistry.unregisterConnector(key.toString());
    }
}
