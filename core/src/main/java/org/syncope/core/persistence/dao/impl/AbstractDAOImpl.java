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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.syncope.core.persistence.beans.AbstractBaseBean;
import org.syncope.core.persistence.dao.DAO;

@Configurable
public abstract class AbstractDAOImpl implements DAO {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(AbstractDAOImpl.class);

    @Value("#{entityManager}")
    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    protected EntityManager entityManager;

    @Override
    public <T extends AbstractBaseBean> T refresh(final T entity) {
        entityManager.refresh(entity);
        return entity;
    }

    @Override
    public void detach(final Object object) {
        entityManager.detach(object);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    @Override
    public void clear() {
        entityManager.clear();
    }
}
