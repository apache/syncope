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
import javassist.NotFoundException;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.ConnectorInstanceLoader;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;

@Repository
public class ConnectorInstanceDAOImpl extends AbstractDAOImpl
        implements ConnectorInstanceDAO {

    @Override
    public ConnectorInstance find(final Long id) {
        return entityManager.find(ConnectorInstance.class, id);
    }

    @Override
    public List<ConnectorInstance> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM ConnectorInstance e");
        return query.getResultList();
    }

    @Override
    public ConnectorInstance save(final ConnectorInstance connector) {
        ConnectorInstance actual = entityManager.merge(connector);
        try {
            ConnectorInstanceLoader.registerConnector(actual);
        } catch (NotFoundException e) {
            LOG.error("While registering the connector for instance "
                    + actual, e);
        }

        return actual;
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));

        ConnectorInstanceLoader.removeConnector(id.toString());
    }
}
