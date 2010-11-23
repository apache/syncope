/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;

@Repository
public class SyncopeConfigurationDAOImpl extends AbstractDAOImpl
        implements SyncopeConfigurationDAO {

    @Override
    public SyncopeConfiguration find(final String name)
            throws MissingConfKeyException {

        SyncopeConfiguration syncopeConfiguration =
                entityManager.find(SyncopeConfiguration.class, name);

        if (syncopeConfiguration == null) {
            throw new MissingConfKeyException(name);
        }

        return syncopeConfiguration;
    }

    @Override
    public List<SyncopeConfiguration> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeConfiguration e");
        return query.getResultList();
    }

    @Override
    public SyncopeConfiguration save(
            final SyncopeConfiguration syncopeConfiguration) {

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
