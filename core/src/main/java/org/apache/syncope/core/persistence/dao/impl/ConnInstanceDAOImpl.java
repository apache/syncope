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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Query;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ConnInstanceDAOImpl extends AbstractDAOImpl implements ConnInstanceDAO {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Override
    public ConnInstance find(final Long id) {
        return entityManager.find(ConnInstance.class, id);
    }

    @Override
    public List<ConnInstance> findAll() {
        Query query = entityManager.createQuery("SELECT e " + "FROM " + ConnInstance.class.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        final ConnInstance merged = entityManager.merge(connector);

        for (ExternalResource resource : merged.getResources()) {
            try {
                connInstanceLoader.registerConnector(resource);
            } catch (NotFoundException e) {
                LOG.error("While registering connector for resource", e);
            }
        }

        return merged;
    }

    @Override
    public void delete(final Long id) {
        ConnInstance connInstance = find(id);
        if (connInstance == null) {
            return;
        }

        Set<String> resourceNames = new HashSet<String>(connInstance.getResources().size());
        for (ExternalResource resource : connInstance.getResources()) {
            resourceNames.add(resource.getName());
        }
        for (String resourceName : resourceNames) {
            resourceDAO.delete(resourceName);
        }

        entityManager.remove(connInstance);

        connInstanceLoader.unregisterConnector(id.toString());
    }
}
