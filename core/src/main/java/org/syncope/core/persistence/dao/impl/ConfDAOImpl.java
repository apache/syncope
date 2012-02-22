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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.ConfDAO;

@Repository
public class ConfDAOImpl extends AbstractDAOImpl implements ConfDAO {

    @Override
    public SyncopeConf find(final String name)
            throws MissingConfKeyException {

        SyncopeConf result = find(name, null);
        if (result == null) {
            throw new MissingConfKeyException(name);
        }

        return result;
    }

    @Override
    public SyncopeConf find(final String name, final String defaultValue) {
        SyncopeConf syncopeConf =
                entityManager.find(SyncopeConf.class, name);

        if (syncopeConf == null && defaultValue != null) {
            syncopeConf = new SyncopeConf();
            syncopeConf.setKey(name);
            syncopeConf.setValue(defaultValue);
        }

        return syncopeConf;
    }

    @Override
    public List<SyncopeConf> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeConf e");
        return query.getResultList();
    }

    @Override
    public SyncopeConf save(
            final SyncopeConf syncopeConfiguration) {

        return entityManager.merge(syncopeConfiguration);
    }

    @Override
    public void delete(final String name) {
        try {
            entityManager.remove(find(name));
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + name + "'");
        }
    }
}
