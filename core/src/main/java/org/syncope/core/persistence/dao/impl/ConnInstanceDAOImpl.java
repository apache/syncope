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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.ConnInstanceDAO;

@Repository
public class ConnInstanceDAOImpl extends AbstractDAOImpl
        implements ConnInstanceDAO {

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Override
    public ConnInstance find(final Long id) {
        return entityManager.find(ConnInstance.class, id);
    }

    @Override
    public List<ConnInstance> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM ConnInstance e");
        return query.getResultList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        return entityManager.merge(connector);
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));

        connInstanceLoader.unregisterConnector(id.toString());
    }
}
